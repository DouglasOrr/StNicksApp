import json
import csv
import argparse
import gzip
import datetime
import re
import pytz
import sys


ORDINAL_PATTERN = re.compile(r'\b([0-9]+)(st|nd|rd|th)\b')
LONDON = pytz.timezone('Europe/London')


def parse_date_and_time(sdate, stime):
    date_no_ordinal = ORDINAL_PATTERN.sub(r'\1', sdate)
    pdate = datetime.datetime.strptime(date_no_ordinal, '%d %B %Y').date()
    ptime = datetime.datetime.strptime(stime, '%H:%M').time()
    return LONDON.localize(datetime.datetime.combine(pdate, ptime)).isoformat()


def read_column(d, name, required):
    text = d[name].strip()
    if required and not text:
        raise ValueError('Bad sermon record - missing required field "{}"'.format(name))
    return text


def parse(line):
    """Parse a sermon record (excluding ID) from a single line."""
    d = dict(time=parse_date_and_time(line['date'], line['time']),
             passage=line['passage'],
             series=line['series'],
             title=line['title'],
             speaker=line['speaker'],
             audio=line['audio'])
    if not d['audio']:
        raise ValueError('Missing audio for {}'.format(dict(line)))
    if not any((d['passage'], d['series'], d['title'], d['speaker'])):
        raise ValueError('Missing (passage|series|title|speaker) for {}'.format(dict(line)))
    return d


def build_index(old_index, sermons):
    """Run checks, assign IDs, build final document."""
    audio_to_old = {d['audio']: d for d in old_index['sermons']}
    new_id = max((d['id'] for d in old_index['sermons']), default=0)
    sermon_list = []
    nupdated = 0
    audio_to_new = {}
    for sermon in sermons:
        audio = sermon['audio']
        if audio in audio_to_new:
            raise ValueError('Duplicate sermon audio links for {} and {}'.format(audio_to_new[audio], sermon))
        if audio in audio_to_old:
            old = audio_to_old[sermon['audio']]
            sermon = dict(id=old['id'], **sermon)
            nupdated += (sermon != old)
        else:
            new_id += 1
            sermon = dict(id=new_id, **sermon)
        sermon_list.append(sermon)
        audio_to_new[audio] = sermon

    sys.stderr.write('{} added, {} updated, {} removed\n'.format(
        len(audio_to_new.keys() - audio_to_old.keys()),
        nupdated,
        len(audio_to_old.keys() - audio_to_new.keys())))
    return dict(sermons=sermon_list)


def run(tsv, old, new):
    """Generate a new sermons index."""
    with gzip.open(old, 'rt') as f:
        old_sermons = json.load(f)
    with open(tsv) as f:
        lines = csv.DictReader(f, delimiter='\t')
        sermons = build_index(old_sermons, [parse(line) for line in lines])
    with gzip.open(new, 'wt') as f:
        json.dump(sermons, f, sort_keys=True, separators=(',', ':'))


parser = argparse.ArgumentParser(description='Import sermons')
parser.add_argument('tsv', help='Input tsv file')
parser.add_argument('old', help='Previous .json.gz sermon index')
parser.add_argument('new', help='Output .json.gz sermon index')
run(**vars(parser.parse_args()))
