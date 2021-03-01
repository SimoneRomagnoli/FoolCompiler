push 0
lfp
push function0
lfp
push function1
lfp
push function2
lfp
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
push 1
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
print
halt

function0:
cfp
lra
lfp
push 1
add
lw
push 1
beq label0
push 20
push 243
add
b label1
label0:
push 20
label1:
stm
sra
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
push 1
add
lw
push 1
beq label2
push 3
b label3
label2:
push 10
label3:
stm
sra
pop
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
push 1
beq label4
lfp
lfp
push 1
add
lw
push -1
mult
push 1
add
lfp
push 3
add
stm
ltm
lw
ltm
push 1
sub
lw
js
b label5
label4:
lfp
lfp
push 1
add
lw
lfp
push 3
add
stm
ltm
lw
ltm
push 1
sub
lw
js
label5:
stm
sra
pop
pop
pop
pop
sfp
ltm
lra
js