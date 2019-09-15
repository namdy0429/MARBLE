import os
import re
import time
import operator
from glob import glob
from multiprocessing import Pool, Queue
from collections import Counter, OrderedDict
import csv
import pickle

output_dir = "/ssd1/dayen/APIList/"
pattern = re.compile(r"""import[^\S](?P<library>.*?);\n""", re.VERBOSE)

overall_api_dict = Counter({})

root_dir = "/ssd1/dayen/java_repos"
package_list = glob(root_dir+"/*")

def getAPIList(package_dir):
	api_dict = {}
	num_total_file = 0
	num_java_file = 0
	fileList = os.walk(package_dir)
	for path, directories, fileNames in fileList:
		for file_name in fileNames:
			num_total_file += 1
			extension = file_name.split(".")[-1]
			if "java" == extension:
				num_java_file += 1
				try:
					with open(os.path.join(path,file_name)) as f:
						src = f.read()
						matches = pattern.findall(src)
						for m in matches:
							if m not in api_dict.keys():
								api_dict[m] = 1
							# api_dict[m] += 1
				except IOError:
					print "***********************************************"
					print "Could not open file:" + path + "/" + file_name
					print	
	return (package_dir, api_dict, num_total_file, num_java_file)

startt = time.time()
pool = Pool(processes=8)

for (package_dir, sub_api_dict, num_total_file, num_java_file) in pool.imap_unordered(getAPIList, package_list):
	print package_dir
	print "num_total_file: " + str(num_total_file)
	print "num_java_file: " + str(num_java_file)
	print sorted(sub_api_dict, key=sub_api_dict.get, reverse=True)[:10]
	print
	overall_api_dict = overall_api_dict + Counter(sub_api_dict)



print "Total script executed in %f seconds\n" % (time.time() - startt)

ranked_list = [(x, overall_api_dict[x]) for x in sorted(overall_api_dict, key=overall_api_dict.get, reverse=True)]
with open(output_dir + 'per_project_ranked_api.csv','wb') as f:
    csv_out=csv.writer(f)
    csv_out.writerow(['api','num_appeard'])
    for row in ranked_list:
        csv_out.writerow(row)

with open(output_dir + 'api_frequency_dict.pickle', 'wb') as f_out:
    pickle.dump(overall_api_dict, f_out)

with open(output_dir + 'api_frequency_dict.pickle', 'rb') as f_in:
    pickled_dict = pickle.load(f_in)

print overall_api_dict == pickled_dict

# with open(output_dir + "ranked_api.text", "w") as f:
# 	f.write(sorted(overall_api_dict.items(), key=overall_api_dict.get, reverse=True))
	# d.items(), key=operator.itemgetter(1)


