#!python3

import sys

def getAddress(name):
    if name == "distributorAddress":
        return ":7011"
    if name == "agentAddress":
        return ":7015"
    raise Exception ("Unknown address_name: " + name)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise Exception ("expected 2 params: file_name and address_name")

    print(getAddress(sys.argv[2]))
