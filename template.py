def getClusterHtml(cluster_name, num_files, within_similarity, files, codes):
	cluster_html = '''
<div class="row">
          <h4 class="col-lg-12 col-md-12 mb-12">{} ({} files, similarity: {})</h4>
        </div>
        <div class="row">
          <div class="col-lg-4 col-md-6 mb-4">
            <h6>{}</h6>
            <pre class="prettyprint">
{}
            </pre>
          </div>

          <div class="col-lg-4 col-md-6 mb-4">
            <h6>{}</h6>
            <pre class="prettyprint">
{}
            </pre>
          </div>

          <div class="col-lg-4 col-md-6 mb-4">
            <h6>{}</h6>
            <pre class="prettyprint">
{}
            </pre>
          </div>

        </div>
        
	'''.format(cluster_name, num_files, within_similarity, files[0], codes[0], files[1], codes[1], files[2], codes[2])
	return cluster_html

def getPatternHtml(pattern_name, num_partitions, num_files, sequence, cluster_htmls):
	pattern_html = '''
<hr class="col-lg-12 col-md-12 mb-12">
    <div class="row">
        <h3 class="col-lg-12 col-md-12 mb-12">{} ({} partitions, {} files)</h3>
        <br>
        <h4 class="col-lg-12 col-md-12 mb-12">{}</h4>
    </div>
    <hr class="col-lg-12 col-md-12 mb-12">
    <div class="row">
      <div class="col-lg-12">
      {}
      </div>
    </div>

	'''.format(pattern_name, num_partitions, num_files, sequence, cluster_htmls)
	return pattern_html

def getAPIHtml(api_name, pattern_htmls):
	api_html = '''
<!DOCTYPE html>
<html lang="en">

<head>

  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
  <meta name="description" content="">
  <meta name="author" content="">

  <title></title>

  <!-- Bootstrap core CSS -->
  <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
  <script src="https://cdn.jsdelivr.net/gh/google/code-prettify@master/loader/run_prettify.js"></script>

  <script src="https://code.jquery.com/jquery-3.3.1.slim.min.js" integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" crossorigin="anonymous"></script>
  <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js" integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1" crossorigin="anonymous"></script>
  <script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" crossorigin="anonymous"></script>

</head>

<body>

  <!-- Page Content -->
  <div class="container" style="max-width: 1620px">
    <div class="row">
      <h1 class="col-lg-12 col-md-12 mb-12" style="margin-top: 30px;">{}</h1>
    </div>
    {}
  </div>
  <!-- /.container -->


</body>

</html>

	'''.format(api_name, pattern_htmls)
	return api_html
