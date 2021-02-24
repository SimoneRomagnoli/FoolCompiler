push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push function1
lhp
sw
lhp
push 1
add
shp
lhp
push function3
lhp
sw
lhp
push 1
add
shp
push function2
lhp
sw
lhp
push 1
add
shp
lhp
push function4
lhp
sw
lhp
push 1
add
shp
push function5
lhp
sw
lhp
push 1
add
shp
push 4
lhp
sw
lhp
push 1
add
shp
push 9998
lw
lhp
sw
lhp
lhp
push 1
add
shp
push 4
lhp
sw
lhp
push 1
add
shp
push 9997
lw
lhp
sw
lhp
lhp
push 1
add
shp
push 4
lhp
sw
lhp
push 1
add
shp
push 9996
lw
lhp
sw
lhp
lhp
push 1
add
shp
lfp
lfp
push -7
add
lw
stm
ltm
ltm
lw
push 0
add
lw
js
print
halt

function0:
cfp
lra
push 0
stm
sra
pop
sfp
ltm
lra
js

function1:
cfp
lra
lfp
lw
push -1
add
lw
stm
sra
pop
sfp
ltm
lra
js

function2:
cfp
lra
lfp
lw
push -1
add
lw
push 2
add
stm
sra
pop
sfp
ltm
lra
js

function3:
cfp
lra
push 0
stm
sra
pop
sfp
ltm
lra
js

function4:
cfp
lra
push 1
stm
sra
pop
sfp
ltm
lra
js

function5:
cfp
lra
lfp
lw
push -1
add
lw
push 4
add
stm
sra
pop
sfp
ltm
lra
js