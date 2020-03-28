# ElevatorTycoon

## Run

To run from source

```
$ git clone repo 
$ sbt run
```
or build docker 

```
$ sbt docker:publishLocal
$ docker run -p 8080:8080 elevatortycoon:0.1
```

## Endpoints

As user create pickup request

```
$ curl http://localhost:8080/api/elevators/pickup/3/up
```

Sample response pickup:

```
{"floor":3,"goalFloor":3,"id":2}

```

Go from one floor to next (use lift id from pickup request):

```
$ curl http://localhost:8080/api/elevators/2/from/3/go/8
```

To go to more than one floor, use:

```
$ curl -H "Content-Type: application/json" -XPOST http://localhost:8080/api/elevators/ride -d '{"id": 1, "floor": 2, "goalFloor": [3,5]}'
```

Querying the state of the elevators:

```
$ watch -d curl http://localhost:8080/api/elevators 
```