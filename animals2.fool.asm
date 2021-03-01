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
push function2
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
lhp
push function2
lhp
sw
lhp
push 1
add
shp
push function7
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
push function8
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
push function2
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
push function10
lhp
sw
lhp
push 1
add
shp
push function11
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
lfp
push function12
lfp
push function13
push 4
push 6
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
lfp
lfp
push -10
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
lfp
push 30
push 0
lfp
push -8
add
stm
ltm
lw
ltm
push 1
sub
lw
js
b label1
label0:
lfp
push 1
push 10
lfp
push -6
add
stm
ltm
lw
ltm
push 1
sub
lw
js
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
push 0
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

function6:
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

function7:
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

function8:
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

function9:
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

function10:
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

function11:
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

function12:
cfp
lra
push 1
stm
sra
pop
pop
pop
sfp
ltm
lra
js

function13:
cfp
lra
push 2
stm
sra
pop
pop
pop
sfp
ltm
lra
js