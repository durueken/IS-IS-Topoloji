# Nokia SR OS Topology Tool

Nokia SR OS CLI çıktılarından IS-IS komşuluk topolojisi çıkaran yerel Java aracı.

## Başlangıç

```bash
mvn test
```

İlk sürüm, bağımsız alan modelini, Nokia SR OS IS-IS parser'ını ve Draw.io XML ihracını içerir.

Parser, `show router isis all adjacency` ile `show router isis all interface` çıktılarındaki çoklu IS-IS instance'ları, satır taşmalarını ve L1/L2 metric değerlerini okur.
Katman tespitinde `_t3_`, `_h3_`, `_z3_` T3; `_t4_`, `_h4_`, `_z4_` T4 olarak kabul edilir.

## Draw.io dosyası üretme

Bir CLI metin dosyasından veya `.txt`/`.log` dosyaları içeren bir klasörden topoloji üretin:

```bash
mvn -Dmaven.repo.local=target/m2 compile
java -cp target/classes com.nokia.topology.cli.TopologyCli input/ topology.drawio
```
