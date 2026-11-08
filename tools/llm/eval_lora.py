import json, re, sys, time, torch
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel
BASE = "models/LFM3-1.7B"; adapter = sys.argv[1] if len(sys.argv) > 1 and sys.argv[1] != "base" else None
tok = AutoTokenizer.from_pretrained(BASE, trust_remote_code=True)
m = AutoModelForCausalLM.from_pretrained(BASE, trust_remote_code=True, dtype=torch.bfloat16).cuda().eval()
if adapter: m = PeftModel.from_pretrained(m, adapter).merge_and_unload()
tok.padding_side = "left"
ev = [json.loads(l) for l in open("eval.jsonl")]
gold = {x["id"]: json.loads(x["answer"]) for x in ev}
def gen(batch):
    p = [tok.apply_chat_template([{"role": "system", "content": x["system"]}, {"role": "user", "content": x["user"]}],
                                 tokenize=False, add_generation_prompt=True, enable_thinking=False) for x in batch]
    enc = tok(p, return_tensors="pt", padding=True, add_special_tokens=False).to("cuda")
    with torch.no_grad(): y = m.generate(**enc, max_new_tokens=900, do_sample=False)
    return [tok.decode(r[enc["input_ids"].shape[1]:], skip_special_tokens=True) for r in y]
def parse(s):
    s = s.strip(); s = s[s.find("{"): s.rfind("}") + 1]
    try: return json.loads(s)
    except Exception: return None
norm = lambda s: re.sub(r"[^\w]", "", (s or "").lower())
t = time.perf_counter(); outs = []
for i in range(0, len(ev), 10): outs += gen(ev[i:i + 10])
valid = 0; ta = tb = tm = am = dm = 0; dec_a = dec_b = 0
for x, o in zip(ev, outs):
    p = parse(o); g = gold[x["id"]]
    if not p or not isinstance(p.get("tasks"), list): continue
    valid += 1
    ta += len(p["tasks"]); tb += len(g["tasks"]); dec_a += len(p.get("decisions", [])); dec_b += len(g["decisions"])
    # совпадение задач по исполнителю и сроку (грубо: пары assignee/due)
    gs = [(norm(z["assignee"]), norm(z["due"])) for z in g["tasks"]]
    for z in p["tasks"]:
        k = (norm(z.get("assignee")), norm(z.get("due")))
        if k in gs: gs.remove(k); tm += 1
print(f"{adapter or 'base'}: valid JSON {valid}/{len(ev)}, {time.perf_counter()-t:.0f}s")
print(f"tasks: pred {ta} gold {tb} matched(assignee+due) {tm} -> P {tm/max(ta,1):.2f} R {tm/max(tb,1):.2f}; decisions pred {dec_a} gold {dec_b}")
# кошачья расшифровка
src = "\n".join(re.sub(r'^\[[^]]*\] speaker_(\d+): ', r'спикер_\1: ', l.strip()) for l in open('../llm_test/transcript.txt', encoding='utf-8') if l.strip())
x = dict(ev[0]); x["user"] = re.sub(r"(?s)(\n\n).*(\n\nСоставь)", lambda mm: "\n\n" + src + mm.group(2), x["user"], count=1)
print("--- cats:\n" + gen([x])[0])
