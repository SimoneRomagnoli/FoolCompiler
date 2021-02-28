push 0
lhp
push function0
lhp
sw
lhp
push 1
add
shp
push 0
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
push 1
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
push 0
push -1
mult
push 1
add
push 1
beq label0
lfp
lfp
push -4
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
b label1
label0:
lfp
lfp
push -3
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
label1:
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