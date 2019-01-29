L'application est codé en scala (version 2.11).

Les middlewares utilisés sont: Kafka -> Spark Streaming -> Mapr-db

La gestion des offsets est géré par le consumer group dans Kafka.

Les messages qui comportent des erreurs sont stockés dans un topic error

Les messages qui ont une structure json avec des champs supplementaires sont injectés dans la table vertica, les champs supplementaires ne sont pas sauvegardés.

Problemes rencontrés:
