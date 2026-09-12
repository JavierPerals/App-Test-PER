"""Validate the Android question asset; optional editorial warnings are not verdicts.

python tools/validate_questions.py --baseline /path/to/original.json --report report.json
The script never rewrites the question bank.
"""
import argparse
from collections import Counter, defaultdict
from difflib import SequenceMatcher
import json
from pathlib import Path
import re
import unicodedata


def normalized(text):
    text = unicodedata.normalize('NFKD', text.casefold())
    return ' '.join(re.findall(r'[a-z0-9]+', ''.join(
        c for c in text if not unicodedata.combining(c))))


def normalized_answer(text):
    # Preserve signs/operators: +4 and -4 are deliberately different options.
    text = unicodedata.normalize('NFKC', text.casefold()).replace('−', '-')
    return ' '.join(text.split()).rstrip('. ')


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate JSON key: ' + key)
        result[key] = value
    return result


def validate(bank, baseline=None):
    errors, long_correct, similar = [], [], []
    if not isinstance(bank, list):
        return {'errors': ['Root must be an array']}
    if len(bank) != 1000:
        errors.append(f'Expected 1000 questions, found {len(bank)}')
    ids, stems, topics, positions = Counter(), defaultdict(list), Counter(), Counter()
    required = {'id', 'topic', 'question', 'answers', 'correct', 'explanation', 'tag'}
    valid = []
    generic = re.compile(r'en una pregunta de examen|en relaci[oó]n con el per|seg[uú]n el per', re.I)
    for index, q in enumerate(bank):
        if not isinstance(q, dict) or not required <= q.keys():
            errors.append(f'Row {index}: missing Android loader fields')
            continue
        ident = q['id']
        if not all(isinstance(q[k], str) and q[k].strip()
                   for k in ('id', 'topic', 'question', 'explanation', 'tag')):
            errors.append(f'Row {index}: invalid or empty text field')
            continue
        ids[ident] += 1
        topics[q['topic']] += 1
        stems[normalized(q['question'])].append(ident)
        answers = q['answers']
        if (not isinstance(answers, list) or len(answers) != 4
                or not all(isinstance(a, str) and a.strip() for a in answers)):
            errors.append(f'{ident}: expected four nonempty answers')
            continue
        if len({normalized_answer(a) for a in answers}) != 4:
            errors.append(f'{ident}: duplicate answer options')
        correct = q['correct']
        if type(correct) is not int or not 0 <= correct < 4:
            errors.append(f'{ident}: invalid zero-based correct index')
            continue
        positions['ABCD'[correct]] += 1
        texts = [q['question'], q['explanation'], *answers]
        if any(':?' in text or '\ufffd' in text for text in texts):
            errors.append(f'{ident}: malformed punctuation or replacement character')
        if generic.search(q['question']) or re.search(r'\bun/a\b', q['question'], re.I):
            errors.append(f'{ident}: artificial introduction or article')
        if q['question'].count('¿') != q['question'].count('?'):
            errors.append(f'{ident}: unbalanced question marks')
        lengths = [len(a) for a in answers]
        longest_wrong = max(n for i, n in enumerate(lengths) if i != correct)
        if lengths[correct] > 1.5 * longest_wrong and lengths[correct] - longest_wrong > 18:
            long_correct.append({'id': ident, 'lengths': lengths, 'correct': correct})
        valid.append(q)
    errors.extend(f'Duplicate ID: {i}' for i, n in ids.items() if n > 1)
    errors.extend(f'Duplicate stem: {v}' for v in stems.values() if len(v) > 1)
    if len(ids) != 1000:
        errors.append(f'Expected 1000 unique IDs, found {len(ids)}')
    if baseline is not None:
        before = {q['id']: q for q in baseline}
        if set(before) != set(ids):
            errors.append('ID set differs from baseline')
        for q in valid:
            old = before.get(q['id'])
            if old is None:
                continue
            if q.keys() != old.keys():
                errors.append(f"{q['id']}: schema changed")
            for key in ('topic', 'difficulty'):
                if q.get(key) != old.get(key):
                    errors.append(f"{q['id']}: {key} changed")
    stop = set('que de del la el los las un una unos unas se en por para con y o al es son como cual cuando su sus lo si no'.split())
    for i, a in enumerate(valid):
        sa = normalized(a['question'])
        ta = set(sa.split()) - stop
        ca = normalized_answer(a['answers'][a['correct']])
        for b in valid[i + 1:]:
            sb = normalized(b['question'])
            tb = set(sb.split()) - stop
            overlap = len(ta & tb) / max(1, len(ta | tb))
            same_answer = ca == normalized_answer(b['answers'][b['correct']])
            if overlap >= .52 or (same_answer and len(ta & tb) >= 3):
                ratio = SequenceMatcher(None, sa, sb).ratio()
                if overlap >= .66 or ratio >= .78 or (same_answer and overlap >= .35):
                    similar.append({'ids': [a['id'], b['id']], 'overlap': round(overlap, 3),
                                    'same_correct_text': same_answer})
    max_run, run, previous = 0, 0, None
    for q in valid:
        run = run + 1 if q['correct'] == previous else 1
        previous = q['correct']
        max_run = max(max_run, run)
    return {'total': len(bank), 'unique_ids': len(ids), 'topics': dict(topics),
            'correct_positions': dict(sorted(positions.items())), 'max_same_position_run': max_run,
            'errors': errors, 'long_correct_candidates': long_correct,
            'similar_question_candidates': similar}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bank', type=Path, default=Path(__file__).resolve().parents[1] / 'app/src/main/assets/questions.json')
    parser.add_argument('--baseline', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    bank = json.loads(args.bank.read_text(encoding='utf-8'), object_pairs_hook=unique_keys)
    baseline = json.loads(args.baseline.read_text(encoding='utf-8')) if args.baseline else None
    report = validate(bank, baseline)
    if args.report:
        args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: v for k, v in report.items() if not k.endswith('_candidates')}, ensure_ascii=True))
    print('Editorial candidates: long correct =', len(report.get('long_correct_candidates', [])),
          '; similar pairs =', len(report.get('similar_question_candidates', [])))
    raise SystemExit(bool(report['errors']))


if __name__ == '__main__':
    main()
