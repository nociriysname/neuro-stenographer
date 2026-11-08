import json, sys, torch
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import PeftModel
BASE = "models/LFM3-1.7B"
tok = AutoTokenizer.from_pretrained(BASE, trust_remote_code=True)
m = PeftModel.from_pretrained(AutoModelForCausalLM.from_pretrained(BASE, trust_remote_code=True, dtype=torch.bfloat16).cuda().eval(), "lfm3-protocol-lora/final").merge_and_unload()
ev = [json.loads(l) for l in open("eval.jsonl")]
for i in map(int, sys.argv[1:]):
    x = ev[i]
    p = tok.apply_chat_template([{"role": "system", "content": x["system"]}, {"role": "user", "content": x["user"]}], tokenize=False, add_generation_prompt=True, enable_thinking=False)
    enc = tok(p, return_tensors="pt", add_special_tokens=False).to("cuda")
    with torch.no_grad(): y = m.generate(**enc, max_new_tokens=900, do_sample=False)
    out = json.loads(tok.decode(y[0, enc["input_ids"].shape[1]:], skip_special_tokens=True))
    tr = x["user"].split("\n\n")[1]
    print(f"########## пример {i}\n--- РАСШИФРОВКА ({len(tr.splitlines())} реплик):\n{tr}\n--- МОДЕЛЬ:\n{json.dumps(out, ensure_ascii=False, indent=1)}\n--- ЭТАЛОН (gpt-6.1):\n{json.dumps(json.loads(x['answer']), ensure_ascii=False, indent=1)}\n")
