import threading
import time


class Callme3:
    def call(self, msg):
        print(f"[{msg}", end="")
        try:
            time.sleep(1)
        except Exception:
            print("Interrupted")
        print("]")


class Caller3(threading.Thread):
    def __init__(self, target,lock, msg):
        threading.Thread.__init__(self)
        self.target = target
        self.lock = lock
        self.msg = msg
        self.start()

    def run(self):
        with self.lock:
            self.target.call(self.msg)


class Synch3:
    def main(self):
        target1 = Callme3()
        lock = threading.Lock()
        ob1 = Caller3(target1,lock, "Hello")
        ob2 = Caller3(target1,lock, "Synchronized")
        ob3 = Caller3(target1,lock, "World")
        ob1.join()
        ob2.join()
        ob3.join()

                # wait for threads to end
        try:
            ob1.join()
            ob2.join()
            ob3.join()
        except Exception:
            print("Interrupted")


if __name__ == "__main__":
    Synch3().main()
