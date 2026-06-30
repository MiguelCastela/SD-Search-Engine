import threading
import time


class Semaphore:

    def Semaphore(self, val:int):
        self.val = val
        self.lock = threading.Lock()
        self.condition = threading.Condition(self.lock)



    def doWait(self):
        with self.lock:
            while self.val == 0:
                self.condition.wait()
            else:
                self.val -= 1




    def doSignal(self):
        with self.lock:
            self.val += 1
            self.condition.notify()    




    
