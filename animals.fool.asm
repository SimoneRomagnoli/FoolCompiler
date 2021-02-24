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
push function0
lhp
sw
lhp
push 1
add
shp
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
push function0
lhp
sw
lhp
push 1
add
shp
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
push function4
lhp
sw
lhp
push 1
add
shp
lhp
push function5
lhp
sw
lhp
push 1
add
shp
push function6
lhp
sw
lhp
push 1
add
shp
lhp
push function7
lhp
sw
lhp
push 1
add
shp
push function8
lhp
sw
lhp
push 1
add
shp
lhp
push function7
lhp
sw
lhp
push 1
add
shp
push function9
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
lfp
push -8
add
lw
lhp
sw
lhp
push 1
add
shp
push 9993
lw
lhp
sw
lhp
lhp
push 1
add
shp
push 2
push 2
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
push 9997
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
push -10
add
lw
lfp
push -9
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
push 2
push 2
push 2
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
push -12
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
push 0
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

function3:
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

function4:
cfp
lra
lfp
lw
push -3
add
lw
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
stm
sra
pop
sfp
ltm
lra
js

function6:
cfp
lra
lfp
lfp
lw
push -2
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
push 1
beq label0
push -1
b label1
label0:
lfp
lw
push -2
add
lw
label1:
stm
sra
pop
sfp
ltm
lra
js

function7:
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

function8:
cfp
lra
push 5
lfp
lfp
push 1
add
lw
stm
ltm
ltm
lw
push 2
add
lw
js
lfp
lfp
push 1
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
add
bleq label4
push 0
b label5
label4:
push 1
label5:
push 1
beq label2
push -1
b label3
label2:
lfp
lw
push -1
add
lw
lfp
push 1
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
push 9995
lw
lhp
sw
lhp
lhp
push 1
add
shp
label3:
stm
sra
pop
pop
sfp
ltm
lra
js

function9:
cfp
lra
lfp
lfp
push 1
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
push 1
beq label6
push -1
b label7
label6:
lfp
lw
push -1
add
lw
lfp
push 1
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
push 9995
lw
lhp
sw
lhp
lhp
push 1
add
shp
label7:
stm
sra
pop
pop
sfp
ltm
lra
js