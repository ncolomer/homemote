help:
	@echo "See Makefile"

clean:
	docker-compose kill
	docker-compose rm -fv

run:
	docker-compose -f docker-compose.yml up -d
	sbt "runMain io.homemote.Homemote"

