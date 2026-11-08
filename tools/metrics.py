#!/usr/bin/env python3
"""Метрики расшифровки против эталона в формате ТЗ: WER, cpWER-подобная привязка к спикерам, DER, ошибка границ.

    python tools/metrics.py hyp.json ref.json [--collar 0.25]

Нормализация для WER: нижний регистр, ё→е, без пунктуации. Числа не переводятся: если эталон пишет
числа словами, а GigaAM-v3 e2e — цифрами, WER будет завышен (это видно в отчёте отдельной строкой).
"""
import argparse, json, re, sys
import numpy as np

def norm(s):
    s = s.lower().replace('ё', 'е')
    s = re.sub(r"[^\w\s]", " ", s)
    return s.split()

def wer(ref_words, hyp_words):
    r, h = ref_words, hyp_words
    d = np.zeros((len(r) + 1, len(h) + 1), dtype=np.int32)
    d[:, 0] = np.arange(len(r) + 1); d[0, :] = np.arange(len(h) + 1)
    for i in range(1, len(r) + 1):
        for j in range(1, len(h) + 1):
            d[i, j] = min(d[i-1, j] + 1, d[i, j-1] + 1, d[i-1, j-1] + (r[i-1] != h[j-1]))
    return d[-1, -1] / max(1, len(r)), int(d[-1, -1]), len(r)

def frames(utts, n, key):
    spk = sorted({u['speaker_id'] for u in utts})
    m = np.zeros((len(spk), n), dtype=bool)
    for u in utts:
        m[spk.index(u['speaker_id']), u['start_ms'] // 10: u['end_ms'] // 10] = True
    return m, spk

def der(ref, hyp, collar_ms=0):
    n = max([u['end_ms'] for u in ref + hyp]) // 10 + 1
    R, rs = frames(ref, n, 'r'); H, hs = frames(hyp, n, 'h')
    score = np.ones(n, dtype=bool)
    if collar_ms:
        c = collar_ms // 10
        for u in ref:
            for b in (u['start_ms'] // 10, u['end_ms'] // 10):
                score[max(0, b - c): b + c] = False
    from itertools import permutations
    best = None
    k = max(len(rs), len(hs))
    Rp = np.vstack([R, np.zeros((k - len(rs), n), bool)]); Hp = np.vstack([H, np.zeros((k - len(hs), n), bool)])
    for perm in permutations(range(k)):
        Hm = Hp[list(perm)]
        ref_n = Rp.sum(0); hyp_n = Hm.sum(0)
        miss = np.maximum(ref_n - hyp_n, 0); fa = np.maximum(hyp_n - ref_n, 0)
        corr = (Rp & Hm).sum(0); conf = np.minimum(ref_n, hyp_n) - corr
        tot = ref_n[score].sum()
        e = (miss[score].sum() + fa[score].sum() + conf[score].sum()) / max(1, tot)
        if best is None or e < best[0]:
            best = (e, miss[score].sum() / tot, fa[score].sum() / tot, conf[score].sum() / tot, perm)
    return best

def boundaries(ref, hyp):
    errs = []
    for r in ref:
        ov = [(min(r['end_ms'], h['end_ms']) - max(r['start_ms'], h['start_ms']), h) for h in hyp]
        o, h = max(ov, key=lambda x: x[0])
        if o > 0:
            errs.append((abs(r['start_ms'] - h['start_ms']), abs(r['end_ms'] - h['end_ms'])))
    return np.mean([e[0] for e in errs]), np.mean([e[1] for e in errs]), len(errs)

def main():
    ap = argparse.ArgumentParser(); ap.add_argument('hyp'); ap.add_argument('ref'); ap.add_argument('--collar', type=int, default=250)
    a = ap.parse_args()
    hyp = json.load(open(a.hyp))['transcript']; ref = json.load(open(a.ref))['transcript']
    rw = [w for u in ref for w in norm(u.get('norm') or u['text'])]
    hw = [w for u in hyp for w in norm(u['text'])]
    w, errs, n = wer(rw, hw)
    d0 = der(ref, hyp, 0); dc = der(ref, hyp, a.collar)
    bs, be, nb = boundaries(ref, hyp)
    out = dict(file=a.hyp, wer=round(w, 4), word_errors=errs, ref_words=n,
               der=round(d0[0], 4), der_collar=round(dc[0], 4), miss=round(d0[1], 4), fa=round(d0[2], 4), conf=round(d0[3], 4),
               spk_ref=len({u['speaker_id'] for u in ref}), spk_hyp=len({u['speaker_id'] for u in hyp}),
               start_err_ms=round(bs), end_err_ms=round(be), utt_ref=len(ref), utt_hyp=len(hyp))
    print(json.dumps(out, ensure_ascii=False))

if __name__ == '__main__':
    main()
