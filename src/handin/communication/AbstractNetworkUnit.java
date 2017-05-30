package handin.communication;

abstract class AbstractNetworkUnit {
    final int serverPortNumber;

    AbstractNetworkUnit(int serverPortNumber) {
        this.serverPortNumber = serverPortNumber;
    }

}
