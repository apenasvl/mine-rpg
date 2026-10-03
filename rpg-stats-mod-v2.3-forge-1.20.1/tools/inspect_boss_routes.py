"""Extract exact native descriptors and call sites; never asserts runtime coverage."""
import re
INTERESTING = re.compile(r'm_6469_|m_7327_|m_142465_|m_5588_|m_7105_|m_7292_|m_147240_|m_214076_|m_7108_|m_6996_|m_8374_|getSender|enqueueWork|setOwner|m_5602_|m_20219_|m_146870_|m_21153_|m_21152_|m_20270_|damage|Damage|explode|Explosion|addAttributeModifier|addPermanentModifier|addTransientModifier|sendToServer|registerMessage')
def inspect_bytecode(text):
    clazz=None;method=None;descriptor=None;calls=[];out=[]
    def finish():
        if clazz and method and descriptor and calls:
            out.append({'class':clazz,'method':method,'descriptor':descriptor,'calls':sorted(set(calls)), 'status':'needs-owner-and-runtime-verification'})
    for line in text.splitlines():
        match=re.match(r'^(?:public |final |abstract )*(?:class|interface) ([\w.$]+)',line)
        if match:
            finish();clazz=match[1];method=None;descriptor=None;calls=[]
        elif line.startswith('  ') and not line.startswith('    ') and '(' in line and line.rstrip().endswith(';'):
            finish();method=line.split('(',1)[0].split()[-1];descriptor=None;calls=[]
        elif line.strip().startswith('descriptor:'):
            descriptor=line.split(':',1)[1].strip()
        elif '// ' in line and INTERESTING.search(line):
            calls.append(line.split('// ',1)[1])
        elif line=='}':
            finish();method=None;descriptor=None;calls=[]
    finish()
    return out
