# Implementation of Chandy-Lamport Global Snapshot Algorithm

Unexpected failures or shutdowns of machines are to be expected in distributed systems. In order to recover from these interruptions, a snapshot of the state of the processes can be taken to record the state of each process. The unreliability of clocks in distributed systems poses a challenge for the snapshot becuase it cannot simply record the state at a given time. 

The Chandy-Lamport global snapshot algorithm uses logical time to create the global snapshot. ore information on the algorithm can be found [here](https://en.wikipedia.org/wiki/Chandy-Lamport_algorithm). I completed this assignment with a partner for my Distributed Computing course in Spring 2017. 
