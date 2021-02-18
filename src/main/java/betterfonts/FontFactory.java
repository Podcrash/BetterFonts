package betterfonts;

interface FontFactory {
    Font createOpenType(java.awt.Font font);

    Font createOpenType(String name, int size);
}
