# kubernetes-velocity-plugin

A velocity plugin watching for k8s minecraft game server pods to 
add them as velocity game servers.

## Build

First, initialize gradle:

```sh
./gradlew wrapper
```
Then build your project using gradle:
```sh
gradle build
```
The compiled jar will be located at `build/libs/`

## Usage

### Configuration

Just place the jar file into your velocity plugins folder. 
It will create a default config on first startup.

The plugin listens for any pod events in the specified namespace 
with the label `kind`. The following options are available:
- `mc-server`   : A normal game server that will be added to velocity's server list. 
- `lobby-server`: A special kind of game server that will also be added to the attempt-connection order.
                  Velocity uses that list as fallback, or "try" servers. For more information see [the velocity docs](https://docs.papermc.io/velocity/configuration#servers-section).
- `proxy-server`: **NOTE: not implemented yet**. Another velocity instance. Used to forward information between proxies in multi-proxy setups.

### Example Pod config
This is an (incomplete) example Pod configuration. 
The plugin will try to connect to any Pods in the specified namespace (default: `minecraft`)
that have set the label `kind` to either `mc-server`, `lobby-server`, or `proxy-server`.
The plugin will always use port `25565`, so your pods should expose that port to be discoverable by velocity. 


```yaml
apiVersion: v1
kind: Pod
metadata:
  name: static-server
  namespace: minecraft
  labels:
    kind: lobby-server
spec:
  containers:
  - name: paper
    image: ben/paper-docker
    ports:
    - containerPort: 25565
      hostPort:  25565
```
