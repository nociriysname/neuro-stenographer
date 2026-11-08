import json, sys, torch
from datasets import load_dataset
from transformers import AutoTokenizer, AutoModelForCausalLM
from peft import LoraConfig
from trl import SFTConfig, SFTTrainer
BASE = "models/LFM3-1.7B"
tok = AutoTokenizer.from_pretrained(BASE, trust_remote_code=True)
def fmt(x):
    msgs = [{"role": "system", "content": x["system"]}, {"role": "user", "content": x["user"]}]
    return {"prompt": tok.apply_chat_template(msgs, tokenize=False, add_generation_prompt=True, enable_thinking=False),
            "completion": x["answer"] + "<|im_end|>"}
ds = load_dataset("json", data_files={"train": "train.jsonl", "eval": "eval.jsonl"})
ds = ds.map(fmt, remove_columns=ds["train"].column_names)
model = AutoModelForCausalLM.from_pretrained(BASE, trust_remote_code=True, dtype=torch.bfloat16)
peft_cfg = LoraConfig(r=32, lora_alpha=64, lora_dropout=0.05, task_type="CAUSAL_LM",
                      target_modules=["q_proj", "k_proj", "v_proj", "o_proj", "in_proj", "out_proj", "w1", "w2", "w3", "gate_proj"])
cfg = SFTConfig(output_dir="lfm3-protocol-lora", num_train_epochs=2, learning_rate=2e-4, lr_scheduler_type="cosine",
                warmup_steps=10, per_device_train_batch_size=4, gradient_accumulation_steps=2, ddp_find_unused_parameters=False, bf16=True,
                gradient_checkpointing=True, max_length=4096, logging_steps=10, eval_strategy="steps", eval_steps=100,
                save_strategy="epoch", report_to="none", completion_only_loss=True)
tr = SFTTrainer(model=model, args=cfg, train_dataset=ds["train"], eval_dataset=ds["eval"], processing_class=tok, peft_config=peft_cfg)
tr.train()
tr.save_model("lfm3-protocol-lora/final")
