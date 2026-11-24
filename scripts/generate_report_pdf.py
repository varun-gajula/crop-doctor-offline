#!/usr/bin/env python3
"""
generate_report_pdf.py - Converts docs/REPORT.md to styled HTML and renders docs/REPORT.pdf using headless Edge/Chrome.
Cleans raw LaTeX, renders display/inline formulas, and applies publication styling.
"""

import os
import re
import subprocess
import html
import sys

def clean_latex(text):
    # LaTeX symbol replacements
    text = re.sub(r'\\text\{([^}]+)\}', r'\1', text)
    text = re.sub(r'\\frac\{([^}]+)\}\{([^}]+)\}', r'\1 / \2', text)
    text = re.sub(r'\\(?:le|leq)\b', '≤', text)
    text = re.sub(r'\\(?:ge|geq)\b', '≥', text)
    text = re.sub(r'\\times\b', '×', text)
    text = re.sub(r'\\cdot\b', '·', text)
    text = re.sub(r'\\approx\b', '≈', text)
    text = re.sub(r'\\mu\b', 'μ', text)
    text = re.sub(r'\\sigma\b', 'σ', text)
    text = re.sub(r'\\sum\b', 'Σ', text)
    text = re.sub(r'\\in\b', '∈', text)
    text = re.sub(r'\\cup\b', '∪', text)
    text = re.sub(r'\\implies\b', '⇒', text)
    text = re.sub(r'\\circ\b', '°', text)
    # Strip inline $ delimiters
    text = re.sub(r'\$([^$]+)\$', r'\1', text)
    return text

def markdown_to_html(md_text):
    lines = md_text.splitlines()
    html_lines = []
    
    in_code_block = False
    code_lang = ""
    code_lines = []
    
    in_table = False
    table_rows = []
    
    in_list = False
    list_type = "ul"
    
    def format_inline(text):
        # Clean any LaTeX first
        text = clean_latex(text)
        
        # Escape html entities
        text = html.escape(text)
        
        # Code spans `...`
        text = re.sub(r'`([^`]+)`', r'<code>\1</code>', text)
        
        # Bold **...**
        text = re.sub(r'\*\*([^*]+)\*\*', r'<strong>\1</strong>', text)
        
        # Italic *...*
        text = re.sub(r'\*([^*]+)\*', r'<em>\1</em>', text)
        
        # Markdown links [text](url)
        text = re.sub(r'\[([^\]]+)\]\(([^)]+)\)', r'<a href="\2">\1</a>', text)
        
        return text

    def close_table():
        nonlocal in_table, table_rows
        if not in_table or not table_rows:
            return ""
        
        out = ["<table>"]
        header_done = False
        
        for row_idx, row in enumerate(table_rows):
            if re.match(r'^\s*\|?\s*[-:]+\s*\|', row) and not header_done:
                continue
                
            cells = [c.strip() for c in row.strip().strip('|').split('|')]
            if not header_done:
                out.append("  <thead><tr>" + "".join(f"<th>{format_inline(c)}</th>" for c in cells) + "</tr></thead>")
                out.append("  <tbody>")
                header_done = True
            else:
                out.append("    <tr>" + "".join(f"<td>{format_inline(c)}</td>" for c in cells) + "</tr>")
                
        if header_done:
            out.append("  </tbody>")
        out.append("</table>")
        
        in_table = False
        table_rows = []
        return "\n".join(out)

    def close_list():
        nonlocal in_list, list_type
        if not in_list:
            return ""
        tag = "</ol>" if list_type == "ol" else "</ul>"
        in_list = False
        return tag

    i = 0
    while i < len(lines):
        line = lines[i]
        
        # Code fence
        if line.strip().startswith("```"):
            if in_code_block:
                code_content = html.escape("\n".join(code_lines))
                html_lines.append(f"<pre><code class=\"{code_lang}\">{code_content}</code></pre>")
                in_code_block = False
                code_lines = []
            else:
                if in_table:
                    html_lines.append(close_table())
                if in_list:
                    html_lines.append(close_list())
                in_code_block = True
                code_lang = line.strip()[3:].strip()
                code_lines = []
            i += 1
            continue
            
        if in_code_block:
            code_lines.append(line)
            i += 1
            continue

        # Display math $$...$$
        if line.strip().startswith("$$") and line.strip().endswith("$$"):
            if in_list:
                html_lines.append(close_list())
            if in_table:
                html_lines.append(close_table())
            raw_math = line.strip()[2:-2].strip()
            cleaned = clean_latex(raw_math)
            html_lines.append(f"<div class=\"math-block\">{html.escape(cleaned)}</div>")
            i += 1
            continue
            
        # Table row
        if line.strip().startswith("|") and line.strip().endswith("|"):
            if in_list:
                html_lines.append(close_list())
            in_table = True
            table_rows.append(line)
            i += 1
            continue
        elif in_table:
            html_lines.append(close_table())
            
        # Blank line
        if not line.strip():
            if in_list:
                html_lines.append(close_list())
            i += 1
            continue
            
        # Horizontal rule
        if re.match(r'^\s*[-*_]{3,}\s*$', line):
            if in_list:
                html_lines.append(close_list())
            html_lines.append("<hr />")
            i += 1
            continue
            
        # Headings
        m_head = re.match(r'^(#{1,6})\s+(.*)$', line)
        if m_head:
            if in_list:
                html_lines.append(close_list())
            level = len(m_head.group(1))
            heading_text = format_inline(m_head.group(2).strip())
            html_lines.append(f"<h{level}>{heading_text}</h{level}>")
            i += 1
            continue
            
        # Unordered list item
        m_ul = re.match(r'^\s*[-*]\s+(.*)$', line)
        if m_ul:
            if not in_list or list_type != "ul":
                if in_list:
                    html_lines.append(close_list())
                html_lines.append("<ul>")
                in_list = True
                list_type = "ul"
            item_text = format_inline(m_ul.group(1).strip())
            html_lines.append(f"  <li>{item_text}</li>")
            i += 1
            continue
            
        # Ordered list item
        m_ol = re.match(r'^\s*(\d+)\.\s+(.*)$', line)
        if m_ol:
            if not in_list or list_type != "ol":
                if in_list:
                    html_lines.append(close_list())
                html_lines.append("<ol>")
                in_list = True
                list_type = "ol"
            item_text = format_inline(m_ol.group(2).strip())
            html_lines.append(f"  <li>{item_text}</li>")
            i += 1
            continue
            
        # Regular paragraph line
        if in_list:
            html_lines.append(close_list())
            
        is_break = line.endswith("  ")
        p_text = format_inline(line.strip())
        if is_break:
            html_lines.append(f"<p class=\"break\">{p_text}</p>")
        else:
            html_lines.append(f"<p>{p_text}</p>")
            
        i += 1
        
    if in_code_block:
        code_content = html.escape("\n".join(code_lines))
        html_lines.append(f"<pre><code>{code_content}</code></pre>")
    if in_table:
        html_lines.append(close_table())
    if in_list:
        html_lines.append(close_list())
        
    body_html = "\n".join(html_lines)
    
    full_html = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Agri Care AI - Technical Report</title>
<style>
  @page {{
    size: A4;
    margin: 18mm 14mm 18mm 14mm;
    @bottom-right {{
      content: counter(page);
    }}
  }}
  body {{
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    color: #1a1a1a;
    line-height: 1.52;
    font-size: 9.5pt;
    margin: 0;
    padding: 0;
  }}
  h1 {{
    font-size: 19pt;
    color: #1b5e20;
    margin-top: 0;
    margin-bottom: 4pt;
    border-bottom: 2pt solid #2e7d32;
    padding-bottom: 4pt;
  }}
  h2 {{
    font-size: 13.5pt;
    color: #2e7d32;
    margin-top: 16pt;
    margin-bottom: 6pt;
    border-bottom: 1pt solid #c8e6c9;
    padding-bottom: 2pt;
    page-break-after: avoid;
  }}
  h3 {{
    font-size: 11pt;
    color: #388e3c;
    margin-top: 11pt;
    margin-bottom: 4pt;
    page-break-after: avoid;
  }}
  p {{
    margin-top: 0;
    margin-bottom: 5pt;
    text-align: justify;
  }}
  p.break {{
    margin-bottom: 2pt;
  }}
  hr {{
    border: none;
    border-top: 1pt solid #e0e0e0;
    margin: 10pt 0;
  }}
  table {{
    width: 100%;
    border-collapse: collapse;
    margin: 7pt 0 10pt 0;
    font-size: 8.5pt;
    page-break-inside: auto;
  }}
  tr {{
    page-break-inside: avoid;
    page-break-after: auto;
  }}
  th, td {{
    border: 0.5pt solid #cccccc;
    padding: 3.5pt 5.5pt;
    text-align: left;
    vertical-align: top;
  }}
  th {{
    background-color: #f1f8e9;
    color: #1b5e20;
    font-weight: 600;
  }}
  tr:nth-child(even) td {{
    background-color: #fafafa;
  }}
  pre {{
    background-color: #f5f5f5;
    border: 0.5pt solid #dddddd;
    border-radius: 3pt;
    padding: 5pt 7pt;
    font-family: "Courier New", Courier, monospace;
    font-size: 7.8pt;
    line-height: 1.35;
    overflow-x: auto;
    page-break-inside: avoid;
    margin: 5pt 0 8pt 0;
  }}
  code {{
    font-family: "Courier New", Courier, monospace;
    font-size: 8.2pt;
    background-color: #f0f0f0;
    padding: 1pt 3pt;
    border-radius: 2pt;
  }}
  pre code {{
    background-color: transparent;
    padding: 0;
  }}
  ul, ol {{
    margin-top: 0;
    margin-bottom: 7pt;
    padding-left: 16pt;
  }}
  li {{
    margin-bottom: 2pt;
  }}
  a {{
    color: #2e7d32;
    text-decoration: none;
  }}
  .math-block {{
    text-align: center;
    font-size: 9.5pt;
    margin: 8pt auto;
    padding: 6pt 10pt;
    background-color: #f1f8e9;
    border: 0.5pt solid #c8e6c9;
    border-radius: 4pt;
    color: #1b5e20;
    width: 85%;
    font-weight: 600;
  }}
</style>
</head>
<body>
{body_html}
</body>
</html>"""
    return full_html

def main():
    md_path = os.path.abspath("docs/REPORT.md")
    html_path = os.path.abspath("docs/REPORT.html")
    pdf_path = os.path.abspath("docs/REPORT.pdf")
    
    if not os.path.exists(md_path):
        print(f"Error: {md_path} not found.")
        sys.exit(1)
        
    with open(md_path, "r", encoding="utf-8") as f:
        md_text = f.read()
        
    html_content = markdown_to_html(md_text)
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
    print(f"Generated {html_path} ({len(html_content)} bytes).")
    
    edge_paths = [
        r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
        r"C:\Program Files\Microsoft\Edge\Application\msedge.exe",
        r"C:\Program Files\Google\Chrome\Application\chrome.exe"
    ]
    
    browser_exe = None
    for p in edge_paths:
        if os.path.exists(p):
            browser_exe = p
            break
            
    if not browser_exe:
        print("Notice: No compatible Chromium browser found for automated PDF export.")
        print(f"Rendered markdown to {html_path}. User can open and print to PDF.")
        sys.exit(0)
        
    print(f"Using browser: {browser_exe}")
    file_url = f"file:///{html_path.replace(os.sep, '/')}"
    
    cmd = [
        browser_exe,
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        f"--print-to-pdf={pdf_path}",
        file_url
    ]
    
    try:
        res = subprocess.run(cmd, capture_output=True, text=True, timeout=40)
        if os.path.exists(pdf_path) and os.path.getsize(pdf_path) > 0:
            print(f"SUCCESS: Generated {pdf_path} ({os.path.getsize(pdf_path):,} bytes).")
        else:
            print(f"Browser ran but {pdf_path} was not created. Stdout: {res.stdout}, Stderr: {res.stderr}")
    except Exception as e:
        print(f"Warning: Failed to render PDF: {e}")

if __name__ == "__main__":
    main()
