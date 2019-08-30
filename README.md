# BotHunter

A Twitter bot detection solution for the [Bots & Gender Profiling](https://pan.webis.de/clef19/pan19-web/author-profiling.html) shared task organised at the [PAN workshop](https://pan.webis.de/) at [CLEF 2019](http://clef2019.clef-initiative.eu/) conference.

The tool was developed within the [HOMADOS](https://homados.ipipan.waw.pl/) project at the [Institute of Computer Science](https://ipipan.waw.pl/), Polish Academy of Sciences. For the description of the approach and evaluation results, see the publication:

Przybyła, P.: [Detecting Bot Accounts on Twitter by Measuring Message Predictability - Notebook for PAN at CLEF.](<http://ceur-ws.org/Vol-2380/paper_58.pdf>) In: Cappellato, L., Ferro, N., Losada, D. E., and Müller, H. (eds.) CLEF 2019 Working Notes. Springer.

You can run the code producing results as described in the paper in the following way (set `<lang>` to `en` for English or `es` for Spanish):
- for LASSO version:

`java -jar ./BotHunter-1.6-jar-with-dependencies.jar GLM <input>/<lang> <output> <workdir> ./R ./modelR <lang>`
- for BERT version

`java -jar ./BotHunter-1.6-jar-with-dependencies.jar BERTpart <input>/<lang> <output> <workdir> <bert-source> <bert-model>/<lang> <venv> ./R ./modelR <lang>`
- for LASSO+BERT combination:

`java -jar ./BotHunter-1.6-jar-with-dependencies.jar GLMBERTpart <input>/<lang> <output> <workdir> <bert-source> <bert-model>/<lang> <venv> ./R ./modelR <lang>`

This requires the following elements to be present:
- a JAR file of BotHunter 1.6 compiled from the source available here (`BotHunter-1.6-jar-with-dependencies.jar`),
- R source files available here (`R`),
- R model files to be [downloaded separately](http://homados.ipipan.waw.pl/bothunter-data/modelR.zip) (`modelR`).

Additionally, the BERT-powered variants require:
- BERT [source code](https://github.com/google-research/bert), including `run_classifier.py` (`<bert-source>`),
- BERT models tuned for the task to be [downloaded separately](http://homados.ipipan.waw.pl/bothunter-data/bert-model.zip) (`<bert-model>`),
- a virtual environment configured with Python and TensorFlow to run BERT (`<venv>`).

If you want to train the approach on a different dataset, the necessary procedures for preparing data (`BotHunter`->`Main.java`) and building ML models (`R`->`TIRA_build.R`) are available here, too.


