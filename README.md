# COSC 650 Socket Programming

A course project that simulates a client and a server sending datagrams to each other.

> I use `devenv` to take care of all the listed requirements. Install [nix](https://determinate.systems/nix-installer/) 😈

## Requirements 

- [just](https://github.com/casey/just): Makefile alternative
- openjdk 21.0.4

### Running the project

> Server asks too many questions

Refer to the `justfile`

- `just default`: Compiles and runs both the server and client programs
- `just compile`: Compiles both the server and client in the `bin` dir
- `just server`: Compiles the server in the `bin` dir
- `just client`: Compiles the client in the `bin` dir
- `just clean`: Cleans up the `bin` dir
