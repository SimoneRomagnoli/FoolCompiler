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
lfp
push function2
lfp
push 2
push 1
lfp
push -3
add
stm
ltm
lw
ltm
push 1
sub
lw
js
lfp
lfp
push -5
add
lw
stm
ltm
ltm
lw
push 1
add
lw
js
print
halt

function0:
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

function1:
cfp
lra
lfp
lw
push -2
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
push 1
add
lw
lfp
push 2
add
lw
lhp
sw
lhp
push 1
add
shp
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
stm
sra
pop
pop
pop
sfp
ltm
lra
js