#! /usr/bin/env python

# reorder the question numbers to be continuous integers

in_f  = open('Test-T40.judgment')
out_f = open('Test-T40.judgment.reordered', 'w')

old_id = ''
counter = 0
for line in in_f:
    splits = line.split()
    id = splits[0]
    if id != old_id:
        counter += 1
    splits[0] = str(counter)
    out_f.write(" ".join(splits)+"\n")
    old_id = id

in_f.close()
out_f.close()
