from argparse import ArgumentParser
import re
from collections import Counter

def sublist(lst1, lst2):
    is_sublist = True
    if not all(elem in lst2 for elem in lst1):
        return False
    if len(lst1) > len(lst2):
        return False
    counter1 = dict(Counter(lst1))
    counter2 = dict(Counter(lst2))
    for w in counter1.keys():
        if counter2[w] < counter1[w]:
            return False
    return True
    # return all(elem in lst2 for elem in lst1) and len(lst1) < len(lst2)

def checkOrder(lst1, lst2):
    prev_idx = lst2.index(lst1[0])
    for l in lst1[1:]:
        cur_idx = lst2.index(l)
        if cur_idx - prev_idx < 0:
            return False
        prev_idx = cur_idx
    return True

def removeSubSequences(input_file, output_file, min_node):
    is_patterns = False
    is_sequence = False
    is_stat = False
    pattern_nums = []
    pattern_counts = {}
    super_pattern_map = {}
    pattern_map = {}
    sublist_map = {}

    with open(input_file, "r") as f_in:
        for line in f_in:
            if line.strip() == "============= INTERESTING SEQUENCES =============":
                is_patterns = True
                pattern_num = -1
            if line.strip() == "============= Filtered SEQUENCES =============":
                break

            if is_patterns:
                pattern = re.search(r"""pattern[\s](?P<number>.*)""", line.strip())

                if is_stat:
                    pattern_counts[pattern_num] = line.strip().split("count: ")[1]
                    is_stat = False

                if is_sequence:
                    sequence_str = line.strip()[1:-1]
                    sequences = sequence_str.split(", ")
                    pattern_counts[pattern_num] = -1
                    pattern_map[pattern_num] = sequences
                    for s in sequences:
                        if s not in super_pattern_map.keys():
                            super_pattern_map[s] = []
                        super_pattern_map[s].append(pattern_num)
                    is_sequence = False
                    is_stat = True

                if pattern:
                    pattern_num = pattern.group('number')
                    is_sequence = True

    filtered_nums = []
    for i, k in enumerate(list(pattern_map.keys())):
        is_subsequence = False
        candidates = super_pattern_map[pattern_map[k][0]]
        if int(pattern_counts[k]) < min_node:
            is_subsequence = True
        else:
            for j, c in enumerate(candidates):
                if k == c:
                     continue
                elif pattern_map[k] == pattern_map[c]:
                    if int(pattern_counts[k]) < int(pattern_counts[c]):
                        is_subsequence = True
                        break
                    else:
                        continue
                if sublist(pattern_map[k], pattern_map[c]):
                    if checkOrder(pattern_map[k], pattern_map[c]):
                        if int(pattern_counts[c]) >= min_node:
                            if float(pattern_counts[c]) / float(pattern_counts[k]) > 0.5:
                                is_subsequence = True
                            if c not in sublist_map.keys():
                                sublist_map[c] = []
                            sublist_map[c].append(k)
        if not is_subsequence:
            filtered_nums.append(k)

    for i in range(len(filtered_nums)):
        k = filtered_nums[i]
        if len(pattern_map[k]) - len(list(set(pattern_map[k]))) >= 3:
            sublists = sorted(sublist_map[k], reverse=True)
            for s in sublists:
                if len(pattern_map[s]) - len(list(set(pattern_map[s]))) < 3:
                    filtered_nums[i] = s
                    print(pattern_map[s])
                    break

    is_patterns = False
    is_sequence = False
    is_stat = False
    with open(output_file, "w") as f_out:
        with open(input_file, "r") as f_in:
            for line in f_in:
                if not is_patterns:
                    f_out.write(line)
                    if line.strip() == "============= INTERESTING SEQUENCES =============":
                        is_patterns = True
                else:
                    pattern = re.search(r"""pattern[\s](?P<number>.*)""", line.strip())
                    pattern_num = -1
                    if pattern:
                        is_sequence = False
                        pattern_num = pattern.group('number')
                        if pattern_num in filtered_nums:
                            is_sequence = True

                    if is_sequence:
                        f_out.write(line)
                    if line.strip() == "============= Filtered SEQUENCES =============":
                        break



if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("-i", "--input", dest="input_file")
    parser.add_argument("-o", "--output", dest="out_file")
    parser.add_argument("-mn", "--min_node", dest="min_node", type=int)

    args = parser.parse_args()

    removeSubSequences(args.input_file, args.out_file, args.min_node)
