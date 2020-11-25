rovers on Mars

## scenario program and player program

- goal of the rover is to score highest by visiting most points
- rover has a radius of vision, where it sees points and chooses which one to move towards
- when sceanrio program runs the simulation process, on every loop it sends data - rover's state and landscape (points, fields, traps, energy recharging etc. - those can be points themselves)
- player program has an operation, that receives reovers state and ladscape within vision state
- player program makes decisions and answers with the next tile (position on the map) that rover chooses to go
- scenario program gets the response (next choses position) and computes next state of the game nad rover
  - rover may have chosen the tile which drains energy
  - or scanned instead of moving, and also spent energy, reducing it's range
  - or recharged energy at a certain point
  - new state of the rover has new energy level and range
- in the next loop, sceanrio program asks for the next move again
- and rover moves in this manner until it has no energy/range
- the score is how many points it visited (but we also see other stats - distance convered, enrgy spent/rechagred
- now, we players see the whole map from the start
- and although we cannot use that data when scenario program asks for next move, it's still possible to program rover to move in the general direction we see visually on the map, but rover does not

## simulation cycle: write program - rover takes 10 steps - adjust program - next 10 steps

- we write a program (logic to make decison about next position) for 1min
- then scenario runs simulation for 10 steps/ticks - we see it on the map
- we have another minute to adjust rover's program for the next part of the journey
- write program - rover takes 10 steps - adjust program - next 10 steps - ...

## distance and energy instead of paths

- rovers sees locations and visits them
- energy spent is proportional to distance (and also field interference - so line and circle intersection)
- visting locations changes rover's state