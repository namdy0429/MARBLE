def compose_message_html(name, api, repo, sha1, sha2=None):
  commit_link1 = "https://github.com/" + repo + "/commit/" + sha1
  
  message = """
<html>
<head></head>
<body>
<p>Hi{},</p>

<p>I'm Daye, a researcher at Carnegie Mellon University studying boilerplate in API client code <a href="https://dayenam.com/MARBLE/">(https://dayenam.com/MARBLE/).</a></p>

<p>I've seen that you probably have a lot of experience with {} from your GitHub contributions to <a href="{}">{}</a></br> (e.g., <a href="{}">{}</a>""".format(name, api, "https://github.com/" + repo, repo, commit_link1, commit_link1)

  if sha2 != None:
    commit_link2 = "https://github.com/" + repo + "/commit/" + sha2
    message = message + """ or <a href="{}">{}</a>""".format(commit_link2, commit_link2)

  message = message + """). </p>

<p><strong>What boilerplate have you encountered when using {}?</strong></p>

<p>Please reply to this email or use this link (<a href="https://www.surveymonkey.com/r/2S3VV2K">https://www.surveymonkey.com/r/2S3VV2K</a>) to share. 
A detailed explanation of why you think the example is boilerplate is always helpful, but all answers are appreciated. 
This is part of a research study and answering our questions might take more than 10 minutes. 
Participation in this research is voluntary, and we will keep your specific API examples confidential if requested.</p>

<p>Sincerely,</br>
Daye Nam</p>
<p><a href="https://cmustrudel.github.io">https://cmustrudel.github.io</a></br><a href="https://dayenam.com">https://dayenam.com</a></p>
</body>
</html>
  """.format(api)

  return message
   
def compose_message_text(name, api, repo, sha1, sha2=None):
  commit_link1 = "https://github.com/" + repo + "/commit/" + sha1
  
  
  message = """Hi {},

I'm Daye, a researcher at Carnegie Mellon University studying boilerplate in API client code (dayenam.com/MARBLE/). 

I've seen that you probably have a lot of experience with {} from your GitHub contributions to {}\n (e.g., {}""".format(name, api, "https://github.com/" + repo, commit_link1)
  if sha2 != None:
    commit_link2 = "https://github.com/" + repo + "/commit/" + sha2
    message = message + """ or {}""".format(commit_link2)

  message = message + """).\n
  *What boilerplate have you encountered when using {}?*

Please reply to this email or use this link (https://www.surveymonkey.com/r/2S3VV2K) to share. A detailed explanation of why you think the example is boilerplate is always helpful, but all answers are appreciated. This is part of a research study and answering our questions might take more than 10 minutes. Participation in this research is voluntary, and we will keep your specific API examples confidential if requested.

Sincerely,
Daye Nam
cmustrudel.github.io
dayenam.com

  """.format(api)

  return message

