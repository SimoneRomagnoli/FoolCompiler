push 0
lfp
push function0
lfp
push function1
push 1
lfp
push -6
add
lw
push 1
beq label0
lfp
push -4
add
stm
ltm
lw
ltm
push 1
sub
lw
b label1
label0:
lfp
push -2
add
stm
ltm
lw
ltm
push 1
sub
lw
label1:
print
halt

function0:
cfp
lra
lfp
push 1
add
lw
stm
sra
pop
pop
pop
sfp
ltm
lra
js

function1:
cfp
lra
lfp
push 2
add
lw
stm
sra
pop
pop
pop
sfp
ltm
lra
js