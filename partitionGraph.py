from argparse import ArgumentParser
import community
import networkx as nx
from glob import glob
import numpy as np

min_node_per_cluster = 5
max_partition = 6
min_node = 0

def getClusters(rood_dir):
	out_file = rood_dir + "MARBLE_result.txt"
	diff_dir = rood_dir + "diff/"
	package_list = glob(diff_dir+ "pattern" + "_*.edgelist")
	sorted_list = sorted(package_list, key=lambda x: int(x.split(".edgelist")[0].split("pattern_")[1]))

	with open(out_file, "w") as out:
		for p in sorted_list:
			p_index = p.split("/")[-1].replace(".edgelist", "").replace("pattern_", "")
			print(p_index)
			isTable = False
			file_dict = {}
			isContinue = False
			pattern = ""
			with open(diff_dir + "Similarity_" + p_index + ".txt") as f_in:
				for i, line in enumerate(f_in):
					if i == 0:
						pattern = line
						# if len(line.split(", ")) == 1:
						# 	isContinue = True
						# 	break
			if isContinue:
					continue

			with open(diff_dir + "Similarity_" + p_index + ".txt") as f_in:
				for line in f_in:
					if line == "\n":
						continue
					if isTable:
						file_dict[line.split("\t")[0].replace(" ", "")] = line.split("\t")[1].replace(" \n", "")
					if "File Index Table" in line:
						isTable = True

			G = nx.read_weighted_edgelist(p)
			try:
				partition = community.best_partition(G)
			except ZeroDivisionError:
				# out.write("There is no edge.\n\n")
				continue

			# size = float(len(set(partition.values())))
			# pos = nx.spring_layout(G)
			# count = 0.
			# for com in set(partition.values()) :
			#     count = count + 1.
			#     list_nodes = [nodes for nodes in partition.keys() if partition[nodes] == com]
			#     nx.draw_networkx_nodes(G, pos, list_nodes, node_size = 20, node_color = str(count / size))

			# nx.draw_networkx_edges(G, pos, alpha=0.5)
			# plt.show()
			if len(partition.keys()) == 0:
				# out.write("There is no node.\n\n")
				out.write("")
			else:
				num_island = 0
				if len(partition.keys()) >= min_node:	
					num_files_per_cluster = []
					with open(diff_dir + "partition_pattern_" + p_index + ".txt", "w") as f_out:
						for i in range(max(partition.values())+1):
							cur_cluster = [file_dict[k] for k,v in partition.items() if v == i]
							if len(cur_cluster) == 1:
								num_island += 1
							f_out.write("Cluster " + str(i) + "\n")
							#num_files_per_cluster.append(len(cur_cluster))
							#np.random.shuffle(cur_cluster)
							for item in cur_cluster:
								f_out.write("%s\n" % item)
							f_out.write("\n")
						for i in range(max(partition.values())+1):
							cur_cluster = [file_dict[k] for k,v in partition.items() if v == i]
							if len(cur_cluster) >= min_node_per_cluster:
								num_files_per_cluster.append(len(cur_cluster))
					sorted_nums = sorted(num_files_per_cluster, reverse = True)
					sum_nums = 0
					for i in range(len(sorted_nums)):
						if (sum_nums > len(partition.keys()) * 0.7):
							break
						sum_nums += sorted_nums[i]
					if len(num_files_per_cluster) <= max_partition and len(num_files_per_cluster) > 0:
						print(num_files_per_cluster)
					# if len(num_files_per_cluster) <= max_partition and len(num_files_per_cluster) > 0 and float(sum(num_files_per_cluster))/float(len(partition.keys())-num_island) >= 0.7:
						out.write(p+"\n")
						out.write(pattern + "\n")
						out.write("num alive partition: " + str(len(num_files_per_cluster)) + "\n")
						out.write("num alive nodes: " + str(sum(num_files_per_cluster)) + "\n")
						out.write("num partition: " + str(max(partition.values())+1-num_island) + "\n")
						out.write("num nodes: " + str(len(partition.keys())-num_island) + "\n")
						out.write("num partition of 90%: " + str(i) + "\n")
						out.write("\n")
				else:
					out.write("")


if __name__ == '__main__':
	parser = ArgumentParser()
	parser.add_argument("-i", "--rood_dir", dest="rood_dir")

	# rood_dir = "/ssd1/dayen/APIList/data/survey_output/android_database_sqlite/"

	args = parser.parse_args()
	getClusters(args.rood_dir)
