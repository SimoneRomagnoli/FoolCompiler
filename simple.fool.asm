push 0
lfp
push function0
lfp
push function1
lfp
push function2
lfp
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
push 25
lfp
push 1
add
lw
bleq label4
push 0
b label5
label4:
push 1
label5:
push 1
beq label2
push 0
b label3
label2:
push 1
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
beq label6
lfp
push 26
lfp
push 5
add
stm
ltm
lw
ltm
push 1
sub
lw
js
b label7
label6:
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
label7:
stm
sra
pop
pop
pop
pop
pop
pop
sfp
ltm
lra
js