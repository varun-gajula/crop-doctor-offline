import sys
import xml.etree.ElementTree as ET

xml_file = sys.argv[1] if len(sys.argv) > 1 else "benchmarks/ui_test_home.xml"
tree = ET.parse(xml_file)
root = tree.getroot()
for node in root.iter("node"):
    text = node.attrib.get("text", "")
    desc = node.attrib.get("content-desc", "")
    bounds = node.attrib.get("bounds", "")
    if text or desc:
        print(f"text: {text!r:35} | desc: {desc!r:25} | bounds: {bounds}")
