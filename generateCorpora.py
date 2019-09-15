import os
import re
from glob import glob
from multiprocessing import Pool, Queue
from argparse import ArgumentParser
import chardet
import codecs

class API:
	def __init__(self, name, dir_root):
		self.name = name
		self.num_file = 0
		self.pattern = re.compile(r"""import[^\S]{}(?P<library>.*?);\n""".format(name), re.VERBOSE)
		self.api_dir = dir_root+"/"+name.replace(".", "_")
		if not os.path.exists(self.api_dir):
			os.makedirs(self.api_dir)

apis = []

def readAPIList(input_file):
	api_list = []
	try:
		with codecs.open(input_file, 'r', encoding="utf-8") as f:
			api_list = f.read().splitlines()
	except IOError:
		print("Could not read file:" + input_file)
	return api_list

def collectAPIfiles(package_dir):
	fileList = os.walk(package_dir)
	num_files = {}
	num_files["package"] = package_dir
	for api in apis:
		num_files[api.name] = 0
	for path, directories, fileNames in fileList:
		for file_name in fileNames:
			extension = file_name.split(".")[-1]
			if "java" == extension:
				try:
					with open(os.path.join(path,file_name), "rb") as f_in:
						try:
							byte_contents = f_in.read()
							content_encoding = chardet.detect(byte_contents)['encoding']
							if content_encoding == None:
								content_encoding = "utf-8"
							src = byte_contents.decode(content_encoding)
						except UnicodeDecodeError:
							src = f_in.read().decode("utf-8")
						# src = f.read()
						for api in apis:
							if re.search(api.pattern, src):
								if os.path.exists(os.path.join(api.api_dir, package_dir.split("/")[-1]+"_____"+file_name)):
									print("Already in the folder: " +  package_dir.split("/")[-1]+"_____"+file_name)
								else:
									try:
										os.symlink(os.path.join(path,file_name), os.path.join(api.api_dir, package_dir.split("/")[-1]+"_____"+file_name))
										num_files[api.name] += 1
									except OSError as e:
										print("OS error({0}): {1}".format(e.errno, e.strerror))
										print(path)
										print(file_name)
										print() 
							# os.symlink(os.path.join(path,file_name), os.path.join(api.api_dir, file_name))
				except IOError as e:
					print("OS error({0}): {1}".format(e.errno, e.strerror))
					print(path)
					print(file_name)
					print() 
					continue
				
	return num_files

def generateCorpora(api_names, in_dir, out_dir, num_process):
	cur_repos = 0

	package_list = glob(in_dir+"/*")
	num_repos = len(package_list)
	for name in api_names:
		api = API(name, out_dir)
		apis.append(api)

	pool = Pool(processes=num_process)
	for num_files in pool.imap_unordered(collectAPIfiles, package_list):
		print(str(cur_repos) + "/" + str(num_repos))
		print(num_files["package"])
		print(num_files)
		print()
		cur_repos += 1
		for api in apis:
			api.num_file += num_files[api.name]
	for api in apis:
		print(api.name)
		print(api.num_file)
		print()


if __name__ == '__main__':
	parser = ArgumentParser()
	parser.add_argument("-a", "--api", dest="api_list", help="target api name (e.g., javax.xml)")
	parser.add_argument("-i", "--input", dest="in_dir")
	parser.add_argument("-o", "--out_dir", dest="out_dir")
	parser.add_argument("-p", "--num_process", dest="num_process", type=int)

	args = parser.parse_args()

	api_list = readAPIList(args.api_list)
	generateCorpora(api_list, args.in_dir, args.out_dir, args.num_process)


	
