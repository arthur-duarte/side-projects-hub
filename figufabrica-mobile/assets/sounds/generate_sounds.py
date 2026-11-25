"""Create the original procedural, royalty-free sound effects shipped with FiguFábrica."""
import math, random, wave
from pathlib import Path
RATE=22050
OUT=Path(__file__).parent
random.seed(21)
def render(name, duration, sample):
    frames=[]
    for n in range(int(RATE*duration)):
        t=n/RATE
        value=max(-1,min(1,sample(t,duration)))
        frames.append(int(value*0.65*32767))
    with wave.open(str(OUT/f'effect{name}.wav'),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE)
        f.writeframes(b''.join(v.to_bytes(2,'little',signed=True) for v in frames))
def tone(freq, t, dur, gain=.5, wave='sine', slide=0):
    if t<0 or t>dur:return 0
    env=min(1,t*80)*max(0,1-t/dur)**1.7
    hz=freq+slide*(t/dur)
    phase=2*math.pi*hz*t
    v=math.sin(phase) if wave=='sine' else (1 if math.sin(phase)>0 else -1)
    return gain*env*v
render(1,.42,lambda t,d:tone(880,t,d,.55)+tone(1320,t-.055,d-.055,.28))
render(2,.52,lambda t,d:tone(520,t,d,.55,'sine',-440))
render(3,.4,lambda t,d:tone(180,t,.14,.35,'square',-40)+tone(115,t-.18,.20,.3,'square',-20))
render(4,1.05,lambda t,d:tone(220,t,.58,.35,'sine',170)+tone(330,t-.34,.67,.24,'sine',110))
def taps(t, centers, width=.055):
    return sum((random.random()*2-1)*.38*max(0,1-abs(t-c)/width) for c in centers)
render(5,.72,lambda t,d:taps(t,[.08,.20,.34,.51,.62],.04)+tone(78,t-.62,.1,.4,'sine',-28))
render(6,.9,lambda t,d:taps(t,[.05,.15,.28,.37,.49,.58,.71,.81],.085))
render(7,1.0,lambda t,d:sum(tone(2600,t-c,.065,.19)+tone(2450,t-c-.07,.05,.16) for c in [.08,.31,.56,.79]))
render(8,.88,lambda t,d:tone(523,t,.29,.36)+tone(659,t-.19,.3,.31)+tone(784,t-.38,.44,.35))
