# ElevatorTycoon

## Run

To run
```
$ sbt run
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

Querying the state of the elevators:

```
$ watch -d curl http://localhost:8080/api/elevators 
```

