"""Синтетика -> SFT (prompt/completion). Одна модель, тип выжимки задаётся инструкцией."""
import json, glob, random
random.seed(0)
SYSTEM = "Ты — секретарь встреч. Работаешь только с переданной расшифровкой: ничего не выдумывай, имена и сроки бери только из текста."
TASKS = {
 "protocol": ("Составь протокол встречи. Верни JSON: {\"title\", \"summary\", \"decisions\": [...], \"tasks\": [{\"task\", \"assignee\", \"due\"}], \"key_points\": [...]}.",
              lambda p: p),
 "summary": ("Кратко перескажи встречу. Верни JSON: {\"title\", \"summary\"}.",
             lambda p: {"title": p["title"], "summary": p["summary"]}),
 "tasks": ("Выпиши поручения и задачи с исполнителями и сроками. Верни JSON: {\"tasks\": [{\"task\", \"assignee\", \"due\"}]}.",
           lambda p: {"tasks": p["tasks"]}),
 "decisions": ("Выпиши принятые решения и договорённости. Верни JSON: {\"decisions\": [...]}.",
               lambda p: {"decisions": p["decisions"]}),
}
def transcript(dialog):
    return "\n".join(f"спикер_{t['speaker']}: {t['text']}" for t in dialog)
def user_msg(task, dialog):
    return f"Расшифровка (спикер_N — анонимные участники, имена в репликах — упоминания людей):\n\n{transcript(dialog)}\n\n{TASKS[task][0]}"
import lzma, os
here = os.path.dirname(os.path.abspath(__file__))
# 2300 синтетических диалогов (1800 встреч + 500 подкастов) с эталонными протоколами от gpt-6.1, см. gen_*.py
rows = [json.loads(l) for l in lzma.open(os.path.join(here, 'synthetic_dialogs.jsonl.xz'), 'rt', encoding='utf-8')]
items = [x for x in rows if x['kind'] == 'meeting']
pods = [x for x in rows if x['kind'] == 'podcast']
eval_ids = set(json.load(open(os.path.join(here, 'eval_ids.json'))))  # 50 отложенных встреч
evalset = [x for x in items if x['id'] in eval_ids]
trainset = [x for x in items if x['id'] not in eval_ids] + pods
random.shuffle(trainset)
def rows(xs, extra):
    out = []
    for x in xs:
        tasks = ["protocol"] + (random.sample(["summary", "tasks", "decisions"], extra) if extra else [])
        for t in tasks:
            out.append({"task": t, "system": SYSTEM, "user": user_msg(t, x["dialog"]),
                        "answer": json.dumps(TASKS[t][1](x["protocol"]), ensure_ascii=False, separators=(",", ":")),
                        "id": x["id"]})
    return out
tr, ev = rows(trainset, 1), rows(evalset, 0)
for name, r in (("train.jsonl", tr), ("eval.jsonl", ev)):
    with open(name, "w") as f:
        for x in r: f.write(json.dumps(x, ensure_ascii=False) + "\n")
print(len(tr), len(ev))
