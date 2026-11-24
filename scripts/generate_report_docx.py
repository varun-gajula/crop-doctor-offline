#!/usr/bin/env python3
"""
generate_report_docx.py - Generates a publication-grade Word Document (docs/REPORT.docx)
directly from docs/REPORT.md using Python's standard library (zipfile and OpenXML).
Zero external dependencies, 100% compliant with Rule H7.
"""

import os
import re
import sys
import zipfile
from xml.sax.saxutils import escape

def clean_latex(text):
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
    text = re.sub(r'\$([^$]+)\$', r'\1', text)
    return text

def parse_runs(text):
    """
    Parses inline markdown (bold, italic, code, link) into a list of (text, is_bold, is_italic, is_code).
    """
    text = clean_latex(text)
    # Simplify links [text](url) -> text
    text = re.sub(r'\[([^\]]+)\]\([^)]+\)', r'\1', text)
    
    # Tokenize by bold, italic, code
    # Pattern matches **bold**, `code`, *italic*
    tokens = re.split(r'(\*\*[^*]+\*\*|`[^`]+`|\*[^*]+\*)', text)
    runs = []
    for tok in tokens:
        if not tok:
            continue
        if tok.startswith('**') and tok.endswith('**') and len(tok) >= 4:
            runs.append((tok[2:-2], True, False, False))
        elif tok.startswith('`') and tok.endswith('`') and len(tok) >= 2:
            runs.append((tok[1:-1], False, False, True))
        elif tok.startswith('*') and tok.endswith('*') and len(tok) >= 2:
            runs.append((tok[1:-1], False, True, False))
        else:
            runs.append((tok, False, False, False))
    return runs

def make_run_xml(text, is_bold=False, is_italic=False, is_code=False, font_size=22, color="222222"):
    rpr = []
    if is_bold:
        rpr.append('<w:b/>')
    if is_italic:
        rpr.append('<w:i/>')
    if is_code:
        rpr.append('<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/>')
        rpr.append('<w:color w:val="8B0000"/>')
        rpr.append('<w:shd w:val="clear" w:color="auto" w:fill="F4F4F4"/>')
        rpr.append(f'<w:sz w:val="{font_size-2}"/>')
    else:
        if color != "222222":
            rpr.append(f'<w:color w:val="{color}"/>')
        rpr.append(f'<w:sz w:val="{font_size}"/>')
        
    rpr_str = f'<w:rPr>{"".join(rpr)}</w:rPr>' if rpr else ''
    escaped_text = escape(text)
    return f'<w:r>{rpr_str}<w:t xml:space="preserve">{escaped_text}</w:t></w:r>'

def make_paragraph_xml(runs, style="Normal", space_before=0, space_after=120, bullet=False):
    ppr = []
    if style != "Normal":
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if bullet:
        ppr.append('<w:numPr><w:ilvl w:val="0"/><w:numId w:val="1"/></w:numPr>')
        ppr.append('<w:ind w:left="480" w:hanging="240"/>')
    if space_before > 0 or space_after > 0:
        ppr.append(f'<w:spacing w:before="{space_before}" w:after="{space_after}"/>')
        
    ppr_str = f'<w:pPr>{"".join(ppr)}</w:pPr>' if ppr else ''
    runs_str = "".join(make_run_xml(t, b, i, c) for t, b, i, c in runs)
    return f'<w:p>{ppr_str}{runs_str}</w:p>'

def make_heading_xml(text, level=1):
    text = clean_latex(text).strip()
    if level == 1:
        # Title or Main Section
        runs = [(text, True, False, False)]
        return f'<w:p><w:pPr><w:pStyle w:val="Heading1"/><w:spacing w:before="360" w:after="140"/><w:keepNext/></w:pPr>' + \
               make_run_xml(text, is_bold=True, font_size=32, color="1B365D") + '</w:p>'
    elif level == 2:
        return f'<w:p><w:pPr><w:pStyle w:val="Heading2"/><w:spacing w:before="260" w:after="100"/><w:keepNext/></w:pPr>' + \
               make_run_xml(text, is_bold=True, font_size=26, color="2E6B34") + '</w:p>'
    elif level == 3:
        return f'<w:p><w:pPr><w:pStyle w:val="Heading3"/><w:spacing w:before="200" w:after="80"/><w:keepNext/></w:pPr>' + \
               make_run_xml(text, is_bold=True, font_size=23, color="333333") + '</w:p>'
    else:
        return f'<w:p><w:pPr><w:pStyle w:val="Heading4"/><w:spacing w:before="160" w:after="60"/><w:keepNext/></w:pPr>' + \
               make_run_xml(text, is_bold=True, font_size=22, color="444444") + '</w:p>'

def make_table_xml(rows):
    if not rows:
        return ""
    
    xml_rows = []
    # Table properties
    tbl_pr = (
        '<w:tblPr>'
        '<w:tblW w:w="0" w:type="auto"/>'
        '<w:tblBorders>'
        '<w:top w:val="single" w:sz="4" w:space="0" w:color="CCCCCC"/>'
        '<w:bottom w:val="single" w:sz="6" w:space="0" w:color="1B365D"/>'
        '<w:insideH w:val="single" w:sz="4" w:space="0" w:color="E0E0E0"/>'
        '<w:insideV w:val="none"/>'
        '<w:left w:val="none"/>'
        '<w:right w:val="none"/>'
        '</w:tblBorders>'
        '<w:tblCellMar>'
        '<w:top w:w="120" w:type="dxa"/>'
        '<w:bottom w:w="120" w:type="dxa"/>'
        '<w:left w:w="160" w:type="dxa"/>'
        '<w:right w:w="160" w:type="dxa"/>'
        '</w:tblCellMar>'
        '</w:tblPr>'
    )
    
    for r_idx, row in enumerate(rows):
        cells = [c.strip() for c in row.strip().strip('|').split('|')]
        tr_pr = '<w:trPr><w:cantSplit/></w:trPr>'
        is_header = (r_idx == 0)
        
        tc_xml_list = []
        for cell in cells:
            cell_runs = parse_runs(cell)
            if is_header:
                cell_p = f'<w:p><w:pPr><w:spacing w:before="60" w:after="60"/></w:pPr>' + \
                         "".join(make_run_xml(t, True, i, c, font_size=20, color="FFFFFF") for t, b, i, c in cell_runs) + '</w:p>'
                tc_pr = '<w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="1B365D"/><w:vAlign w:val="center"/></w:tcPr>'
            else:
                bg = 'F8F9FA' if r_idx % 2 == 1 else 'FFFFFF'
                cell_p = f'<w:p><w:pPr><w:spacing w:before="40" w:after="40"/></w:pPr>' + \
                         "".join(make_run_xml(t, b, i, c, font_size=20) for t, b, i, c in cell_runs) + '</w:p>'
                tc_pr = f'<w:tcPr><w:shd w:val="clear" w:color="auto" w:fill="{bg}"/><w:vAlign w:val="center"/></w:tcPr>'
            tc_xml_list.append(f'<w:tc>{tc_pr}{cell_p}</w:tc>')
            
        xml_rows.append(f'<w:tr>{tr_pr}{"".join(tc_xml_list)}</w:tr>')
        
    return f'<w:tbl>{tbl_pr}{"".join(xml_rows)}</w:tbl><w:p><w:pPr><w:spacing w:after="120"/></w:pPr></w:p>'

def make_code_block_xml(code_lines):
    p_list = []
    for line in code_lines:
        escaped_line = escape(line)
        p_list.append(
            f'<w:p><w:pPr><w:spacing w:before="0" w:after="0" w:line="240" w:lineRule="auto"/>'
            f'<w:ind w:left="240" w:right="240"/>'
            f'<w:shd w:val="clear" w:color="auto" w:fill="F4F6F8"/></w:pPr>'
            f'<w:r><w:rPr><w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:sz w:val="18"/><w:color w:val="2D3748"/></w:rPr>'
            f'<w:t xml:space="preserve">{escaped_line}</w:t></w:r></w:p>'
        )
    return "".join(p_list) + '<w:p><w:pPr><w:spacing w:after="120"/></w:pPr></w:p>'

def markdown_to_docx_body(md_text):
    lines = md_text.splitlines()
    body_elements = []
    
    in_code = False
    code_lines = []
    
    in_table = False
    table_rows = []
    
    for line in lines:
        stripped = line.strip()
        
        # Code block toggle
        if stripped.startswith('```'):
            if in_code:
                in_code = False
                body_elements.append(make_code_block_xml(code_lines))
                code_lines = []
            else:
                if in_table:
                    body_elements.append(make_table_xml(table_rows))
                    in_table = False
                    table_rows = []
                in_code = True
                code_lines = []
            continue
            
        if in_code:
            code_lines.append(line)
            continue
            
        # Table rows
        if stripped.startswith('|') and stripped.endswith('|'):
            # Skip separator line |---|---|
            if re.match(r'^\s*\|?\s*[-:]+\s*\|', stripped):
                continue
            if not in_table:
                in_table = True
                table_rows = []
            table_rows.append(stripped)
            continue
        else:
            if in_table:
                body_elements.append(make_table_xml(table_rows))
                in_table = False
                table_rows = []
                
        # Empty lines
        if not stripped:
            continue
            
        # Headings
        if stripped.startswith('# '):
            body_elements.append(make_heading_xml(stripped[2:], level=1))
        elif stripped.startswith('## '):
            body_elements.append(make_heading_xml(stripped[3:], level=1))
        elif stripped.startswith('### '):
            body_elements.append(make_heading_xml(stripped[4:], level=2))
        elif stripped.startswith('#### '):
            body_elements.append(make_heading_xml(stripped[5:], level=3))
        # Horizontal rule
        elif stripped in ('---', '***', '___'):
            body_elements.append('<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="CCCCCC"/></w:pBdr><w:spacing w:before="120" w:after="120"/></w:pPr></w:p>')
        # Bullet list item
        elif stripped.startswith('- ') or stripped.startswith('* '):
            runs = parse_runs(stripped[2:])
            body_elements.append(make_paragraph_xml(runs, bullet=True, space_after=60))
        # Numbered list item
        elif re.match(r'^\d+\.\s+', stripped):
            m = re.match(r'^\d+\.\s+(.*)', stripped)
            runs = parse_runs(m.group(1))
            body_elements.append(make_paragraph_xml(runs, bullet=True, space_after=60))
        # Normal paragraph
        else:
            runs = parse_runs(stripped)
            body_elements.append(make_paragraph_xml(runs, space_after=100))
            
    if in_code and code_lines:
        body_elements.append(make_code_block_xml(code_lines))
    if in_table and table_rows:
        body_elements.append(make_table_xml(table_rows))
        
    return "".join(body_elements)

def generate_docx(md_path, docx_path):
    with open(md_path, 'r', encoding='utf-8') as f:
        md_text = f.read()
        
    body_xml = markdown_to_docx_body(md_text)
    
    content_types = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">\n'
        '  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>\n'
        '  <Default Extension="xml" ContentType="application/xml"/>\n'
        '  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>\n'
        '  <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>\n'
        '  <Override PartName="/word/numbering.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml"/>\n'
        '</Types>'
    )
    
    rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\n'
        '  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>\n'
        '</Relationships>'
    )
    
    doc_rels = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">\n'
        '  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>\n'
        '  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/numbering" Target="numbering.xml"/>\n'
        '</Relationships>'
    )
    
    styles = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">\n'
        '  <w:docDefaults>\n'
        '    <w:rPrDefault>\n'
        '      <w:rPr>\n'
        '        <w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:cs="Arial"/>\n'
        '        <w:sz w:val="22"/>\n'
        '        <w:color w:val="222222"/>\n'
        '      </w:rPr>\n'
        '    </w:rPrDefault>\n'
        '    <w:pPrDefault>\n'
        '      <w:pPr>\n'
        '        <w:spacing w:line="276" w:lineRule="auto"/>\n'
        '      </w:pPr>\n'
        '    </w:pPrDefault>\n'
        '  </w:docDefaults>\n'
        '  <w:style w:type="paragraph" w:styleId="Normal" w:default="1">\n'
        '    <w:name w:val="Normal"/>\n'
        '  </w:style>\n'
        '  <w:style w:type="paragraph" w:styleId="Heading1">\n'
        '    <w:name w:val="heading 1"/>\n'
        '    <w:rPr><w:b/><w:sz w:val="32"/><w:color w:val="1B365D"/></w:rPr>\n'
        '  </w:style>\n'
        '  <w:style w:type="paragraph" w:styleId="Heading2">\n'
        '    <w:name w:val="heading 2"/>\n'
        '    <w:rPr><w:b/><w:sz w:val="26"/><w:color w:val="2E6B34"/></w:rPr>\n'
        '  </w:style>\n'
        '  <w:style w:type="paragraph" w:styleId="Heading3">\n'
        '    <w:name w:val="heading 3"/>\n'
        '    <w:rPr><w:b/><w:sz w:val="23"/><w:color w:val="333333"/></w:rPr>\n'
        '  </w:style>\n'
        '</w:styles>'
    )
    
    numbering = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<w:numbering xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">\n'
        '  <w:abstractNum w:abstractNumId="0">\n'
        '    <w:lvl w:ilvl="0">\n'
        '      <w:start w:val="1"/>\n'
        '      <w:numFmt w:val="bullet"/>\n'
        '      <w:lvlText w:val="•"/>\n'
        '      <w:lvlJc w:val="left"/>\n'
        '      <w:pPr><w:ind w:left="480" w:hanging="240"/></w:pPr>\n'
        '      <w:rPr><w:rFonts w:ascii="Arial" w:hAnsi="Arial"/></w:rPr>\n'
        '    </w:lvl>\n'
        '  </w:abstractNum>\n'
        '  <w:num w:numId="1">\n'
        '    <w:abstractNumId w:val="0"/>\n'
        '  </w:num>\n'
        '</w:numbering>'
    )
    
    document = (
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>\n'
        '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">\n'
        '  <w:body>\n'
        f'{body_xml}\n'
        '    <w:sectPr>\n'
        '      <w:pgSz w:w="11906" w:h="16838"/>\n'  # A4 size
        '      <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>\n'
        '    </w:sectPr>\n'
        '  </w:body>\n'
        '</w:document>'
    )
    
    with zipfile.ZipFile(docx_path, 'w', zipfile.ZIP_DEFLATED) as z:
        z.writestr('[Content_Types].xml', content_types)
        z.writestr('_rels/.rels', rels)
        z.writestr('word/_rels/document.xml.rels', doc_rels)
        z.writestr('word/styles.xml', styles)
        z.writestr('word/numbering.xml', numbering)
        z.writestr('word/document.xml', document)
        
    print(f"SUCCESS: Generated {docx_path} ({os.path.getsize(docx_path):,} bytes).")

def main():
    md_path = "docs/REPORT.md"
    docx_path = "docs/REPORT.docx"
    if not os.path.exists(md_path):
        print(f"Error: {md_path} not found.")
        sys.exit(1)
    generate_docx(md_path, docx_path)

if __name__ == "__main__":
    main()
