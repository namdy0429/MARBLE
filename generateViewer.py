import template
from collections import Counter
from argparse import ArgumentParser
import chardet
import codecs
import math
import os

def hasAlphabets(inputString):
	return any(char.isalpha() for char in inputString)

def findLineStart(content, position):
	if position >= len(content) or position <= 0:
		return position
	while content[position] != "\n":
		if position == 0:
			break
		position = position - 1
	return position + 1

def findLineEnd(content, position):
	if position >= len(content) or position <= 0:
		return position
	while content[position] != "\n":
		if position == len(content) - 1:
			break
		position = position + 1
	return position

def generateViewer(input_dir, source_dir, api_name, output_dir, min_nodes, min_similarity, is_pam):
# def generateViewer(input_dir, source_dir, api_name, output_dir, min_nodes, min_similarity):
	dirname = os.path.dirname(__file__)
	seq_min_similarity = [0, 0.67, 0.55, 0.44, 0.37, 0.30, 0.25, 0.25, 0.25, 0.25, 0.25]
	edge_lists = []
	partition_nums = []
	file_nums = []
	boilerplate_files = []

	with open(os.path.join(dirname, input_dir, "MARBLE_result.txt"), "r") as f_in:
		for line in f_in:
			if ".edgelist" in line:
				edge_lists.append(line.strip())
			elif "num alive partition" in line:
				partition_nums.append(line.strip().split("num alive partition: ")[1])
			elif "num alive nodes:" in line:
				file_nums.append(line.strip().split("num alive nodes: ")[1])

	pattern_htmls = ""
	pattern_html_list = []
	mean_length_list = []
	for i, e in enumerate(edge_lists):
		partition_file = "/".join(e.split("/")[:-1]) + "/partition_pattern_" + e.split("pattern_")[1].replace("edgelist", "txt")
		print(partition_file)
		clusters = {}
		pattern_name = e.split("/")[-1].split(".edgelist")[0]
		with codecs.open(partition_file, "r", errors="ignore") as f_in:
			cluster = ""
			for line in f_in:
				if "Cluster " in line:
					cluster = line.strip()
					if cluster not in clusters.keys():
						clusters[cluster] = []
				elif line.strip() != "":
					clusters[cluster].append(line.strip())
		# cluster_htmls = ""
		cluster_html_list = []
		sum_length = 0
		num_edges = 0
		best_within_similarity = 0
		num_stmts = 0
		for c in clusters.keys():
			cluster_name = c
			if len(clusters[c]) < min_nodes:
				continue
			else:
				file_list = []
				code_list = []
				similarity_file = "/".join(e.split("/")[:-1]) + "/Similarity_" + e.split("pattern_")[1].replace("edgelist", "txt")
				sequence, selected_files, num_files, cluster_sum_length, cluster_num_edges, within_similarity, cluster_num_stmts, bp_files = selectFiles(similarity_file, clusters[c], is_pam)
				min_similarity = seq_min_similarity[len(sequence)]
				
				if best_within_similarity < within_similarity:
					best_within_similarity = within_similarity
				for (s, p) in selected_files:
					with open(os.path.join(dirname, source_dir, s + ".java"), "rb") as f_in:
						try:
							byte_contents = f_in.read()
							content_encoding = chardet.detect(byte_contents)['encoding']
							contents = byte_contents.decode(content_encoding)
						except UnicodeDecodeError:
							contents = f_in.read().decode("utf-8")
					file_list.append(s)
					snippet_start = findLineStart(contents, p[0])
					snippet_end = findLineEnd(contents, p[1])
					code_list.append(contents[snippet_start:snippet_end])
			# cluster_htmls = template.getClusterHtml(cluster_name, num_files, file_list, code_list) + cluster_htmls
			# cluster_html_list.append(((cluster_name, num_files, within_similarity, file_list, code_list), num_files))
				min_similarity = seq_min_similarity[len(sequence)]
				if within_similarity >= min_similarity and len(file_list) == 3:
					cluster_html_list.append(((cluster_name, num_files, within_similarity, file_list, code_list), within_similarity))
					sum_length += cluster_sum_length
					num_edges += cluster_num_edges
					num_stmts += cluster_num_stmts		
					boilerplate_files.extend(bp_files)
		cluster_html_list.sort(key=lambda tup: tup[1], reverse=True)
		cluster_htmls = "".join([template.getClusterHtml(x[0][0], x[0][1], x[0][2], x[0][3], x[0][4]) for x in cluster_html_list])
		if num_edges > 0 and cluster_htmls != "":
		# if cluster_htmls != "":
			# mean length
			# pattern_html_list.append((template.getPatternHtml(pattern_name, partition_nums[i], file_nums[i], ", ".join(sequence), cluster_htmls), sum_length / num_edges))
			# best cluster's mean within-similarity score
			# pattern_html_list.append((template.getPatternHtml(pattern_name, partition_nums[i], file_nums[i], ", ".join(sequence), cluster_htmls), best_within_similarity))
			pattern_html_list.append((template.getPatternHtml(pattern_name, partition_nums[i], file_nums[i], ", ".join(sequence), cluster_htmls), num_stmts/num_edges))
	pattern_html_list.sort(key=lambda tup: tup[1], reverse=True)
	print("Num candidate patterns")
	print(len(pattern_html_list))
	api_html = template.getAPIHtml(api_name, "".join([x[0] for x in pattern_html_list]))
	print("Boilerplate Files")
	print(len(list(set(boilerplate_files))))
	# api_html = template.getAPIHtml(api_name, pattern_htmls)
	with codecs.open(os.path.join(dirname, output_dir, api_name + "_index.html"), "w", encoding="utf-8") as f_out:
		f_out.write(api_html)

def selectFiles(similarity_file, cluster, is_pam):
	scores = {}
	positions = {}
	is_positions = False
	is_sequence = True
	sequence = ""
	sum_length = 0
	num_files = 0
	within_similarity = 0
	# within_similarity = []
	num_edges = 0
	num_stmts = 0
	for c in cluster:
		scores[c] = 0
	with open(similarity_file, "r", errors="ignore") as f_in:
		first_node = ""
		second_node = ""
		for line in f_in:
			if is_sequence:
					sequence = line.replace("[","").replace("]","").replace("<", "&lt;").replace(">","&gt;").split(", ")
					is_sequence = False
			elif "File Index Table" == line.strip():
				break
			elif is_positions and first_node != "" and second_node != "":
				first_start_pos, first_end_pos, first_num_stmt, second_start_pos, second_end_pos, second_num_stmt = line.strip().split(", ")
				sum_length = sum_length - int(first_start_pos) + int(first_end_pos) - int(second_start_pos) + int(second_end_pos)
				num_files += 2
				num_stmts = num_stmts + int(first_num_stmt) + int(second_num_stmt)

				if first_node in cluster and second_node in cluster:
					if first_node not in positions.keys():
						positions[first_node] = []
					if second_node not in positions.keys():
						positions[second_node] = []
					positions[first_node].append((int(first_start_pos), int(first_end_pos)))
					positions[second_node].append((int(second_start_pos), int(second_end_pos)))
				first_node = ""
				second_node = ""
				is_positions = False
			elif first_node != "" and second_node != "":
				if first_node in cluster and second_node in cluster:
					scores[first_node] += float(line.strip())
					scores[second_node] += float(line.strip())
					within_similarity += float(line.strip())
					# within_similarity.append(float(line.strip()))
					num_edges += 1
				elif first_node in cluster and second_node not in cluster:
					scores[first_node] -= float(line.strip())
				elif first_node not in cluster and second_node in cluster:
					scores[second_node] = float(line.strip())
				is_positions = True
			# elif "_____" in line:
			elif hasAlphabets(line.strip()) and line.strip() != "":
				if first_node == "":
					first_node = line.strip()
				else:
					second_node = line.strip()
			else:
				continue
	sorted_x = sorted(scores.items(), key=lambda kv: (kv[1], kv[0]), reverse=True)
	# selected_files = [sorted_x[0][0], sorted_x[1][0], sorted_x[2][0]]
	selected_files = []
	selected_projects = []
	num_sf = 0
	if is_pam:
		for i, f in enumerate(sorted_x):
			selected_files.append(f[0])
			num_sf += 1
			if num_sf == 3:
				break
	else:
		for i, f in enumerate(sorted_x):
			p = ("_____").join(f[0].split("_____")[:-1])
			if p not in selected_projects:
				selected_files.append(f[0])
				selected_projects.append(p)
				num_sf += 1
				if num_sf == 3:
					break

	selected_positions = []
	for f in selected_files:
		position_candidates = positions[f]
		selected_positions.append(Counter(position_candidates).most_common(1)[0][0])
	result = list(zip(selected_files, selected_positions))
	return sequence, result, len(scores.keys()), sum_length, num_files, round(within_similarity/num_edges, 3), num_stmts, list(scores.keys())

if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument("-i", "--input_dir", dest="input_dir")
    parser.add_argument("-o", "--output_dir", dest="output_dir")
    parser.add_argument("-s", "--source_dir", dest="source_dir")
    parser.add_argument("-a", "--api_name", dest="api_name")
    parser.add_argument("-mn", "--min_nodes", dest="min_nodes", type=int, default=5)
    parser.add_argument("-ms", "--min_similarity", dest="min_similarity", type=float)
    parser.add_argument("-ip", "--is_pam", dest="is_pam", action='store_true')

    args = parser.parse_args()
    generateViewer(args.input_dir, args.source_dir, args.api_name, args.output_dir, args.min_nodes, args.min_similarity, args.is_pam)
