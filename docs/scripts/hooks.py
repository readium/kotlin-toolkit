import re

def on_page_markdown(markdown, page, config, files):
    # Only do this for the links on the index page
    if page.file.src_path == "index.md":
        # This fixes the depth issue since index.md is already inside /docs
        markdown = re.sub(r'(\[.*?\])\(docs/(.*?\.md)\)', r'\1(\2)', markdown)

    return markdown
