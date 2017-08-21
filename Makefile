help:
	@echo "See Makefile"

clean:
	docker-compose kill
	docker-compose rm -fv

run:
	docker-compose up -d
	sbt "runMain io.homemote.Homemote"

test:
	docker-compose up -d
	sbt test

deb:
	sbt debian:packageBin
