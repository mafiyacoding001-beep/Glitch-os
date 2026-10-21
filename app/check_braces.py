
import sys

def check_braces(filename):
    with open(filename, 'r') as f:
        content = f.read()
    
    stack = []
    line_num = 1
    in_string = False
    in_single_comment = False
    in_multi_comment = False
    
    i = 0
    while i < len(content):
        char = content[i]
        
        if in_single_comment:
            if char == '\n':
                in_single_comment = False
                line_num += 1
        elif in_multi_comment:
            if char == '*' and i + 1 < len(content) and content[i+1] == '/':
                in_multi_comment = False
                i += 1
            if char == '\n':
                line_num += 1
        elif in_string:
            if char == '"' and content[i-1] != '\\':
                in_string = False
            if char == '\n':
                line_num += 1
        else:
            if char == '"':
                in_string = True
            elif char == '/' and i + 1 < len(content) and content[i+1] == '/':
                in_single_comment = True
                i += 1
            elif char == '/' and i + 1 < len(content) and content[i+1] == '*':
                in_multi_comment = True
                i += 1
            elif char == '{':
                stack.append(line_num)
            elif char == '}':
                if stack:
                    stack.pop()
                else:
                    print(f"Extra closing brace at line {line_num}")
                    return
            if char == '\n':
                line_num += 1
        i += 1
            
    if stack:
        print(f"Unclosed opening braces at lines: {stack}")
    else:
        print("Braces are balanced")

if __name__ == "__main__":
    check_braces(sys.argv[1])
