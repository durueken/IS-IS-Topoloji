# Nokia SR OS Topology Tool

# IS-IS Topoloji Otomasyonu

Nokia SR OS cihazlarından alınan IS-IS CLI çıktılarını ayrıştırarak otomatik ve düzenlenebilir Draw.io ağ topolojisi oluşturan Java uygulamasıdır.

## Projenin Amacı

Operatör ağlarında farklı cihazlardan alınan IS-IS adjacency ve interface çıktılarının manuel olarak incelenmesi ve bağlantıların elle çizilmesi zaman alabilmektedir. Bu proje, CLI çıktılarındaki cihaz ve bağlantı bilgilerini otomatik olarak işleyerek Draw.io ortamında açılabilen bir topoloji dosyası üretir.

## Özellikler

- Nokia SR OS IS-IS CLI çıktılarının ayrıştırılması
- Bir veya birden fazla `.txt` ve `.log` dosyasının işlenmesi
- Hostname, System ID ve interface bilgilerinin okunması
- IS-IS instance, L1/L2 seviye, state ve metric bilgilerinin ayrıştırılması
- Alt satıra taşan uzun interface adlarının birleştirilmesi
- Karşılıklı adjacency kayıtlarının tek bağlantıda birleştirilmesi
- Paralel LAG bağlantılarının korunması
- Cihazların T2 Core, T3 Aggregation ve T4 Access katmanlarına ayrılması
- Katmanlı ve kuvvet yönlendirmeli otomatik yerleşim
- Draw.io uyumlu XML çıktısının oluşturulması
- Birim ve uçtan uca test desteği

## Desteklenen Komutlar

Uygulama aşağıdaki Nokia SR OS komutlarından alınan çıktıları işler:

```text
show router isis all adjacency
show router isis all interface
```

## Gereksinimler

- Java 21 veya üzeri
- Apache Maven
- Üretilen dosyayı görüntülemek için Draw.io

Sürümleri kontrol etmek için:

```bash
java -version
mvn -version
```

## Projeyi Derleme

Proje klasöründe aşağıdaki komutu çalıştırın:

```bash
mvn compile
```

Bağımlılıkların proje içerisindeki geçici klasörde tutulması istenirse:

```bash
mvn -Dmaven.repo.local=target/m2 compile
```

## Testleri Çalıştırma

```bash
mvn test
```

Proje; parser, topoloji oluşturma, yerleşim, XML üretimi ve komut satırı çalışma akışını kontrol eden testler içerir.

## Kullanım

Uygulama tek bir CLI dosyasıyla çalıştırılabilir:

```bash
java -cp target/classes com.nokia.topology.cli.TopologyCli input.txt topology.drawio
```

Bir klasör içerisindeki bütün `.txt` ve `.log` dosyaları birlikte de işlenebilir:

```bash
java -cp target/classes com.nokia.topology.cli.TopologyCli input/ topology.drawio
```

Komut tamamlandığında terminalde oluşturulan cihaz ve bağlantı sayısı gösterilir:

```text
Topoloji üretildi: topology.drawio (14 cihaz, 13 bağlantı)
```

## Çıktı

Oluşturulan `.drawio` dosyası Draw.io üzerinde açılabilir ve düzenlenebilir. Çıktıda:

- Cihazlar ağ katmanlarına göre farklı şekil ve renklerle gösterilir.
- Bağlantılarda IS-IS instance, seviye ve metric bilgileri bulunur.
- Kaynak ve hedef interface bilgileri bağlantı uçlarında gösterilir.
- Cihaz konumları otomatik olarak hesaplanır.

## Proje Yapısı

```text
src/
├── main/java/com/nokia/topology/
│   ├── cli/       Komut satırı giriş noktası
│   ├── domain/    Cihaz, bağlantı ve topoloji modelleri
│   ├── engine/    Topoloji oluşturma ve bağlantı birleştirme
│   ├── export/    Draw.io XML üretimi
│   ├── layout/    Cihaz yerleşim algoritması
│   └── parser/    Nokia SR OS CLI ayrıştırma işlemleri
└── test/
    ├── java/      Birim ve uçtan uca testler
    └── resources/ Anonimleştirilmiş test girdileri
```

## Bilgi Güvenliği

Bu depoda kuruma ait gerçek CLI çıktıları, cihaz adları, konum bilgileri, interface açıklamaları veya gerçek topoloji dosyaları bulunmamaktadır. Testlerde yalnızca anonimleştirilmiş örnek veriler kullanılmaktadır.

Üretilen `.drawio` dosyaları ve Maven tarafından oluşturulan `target` klasörü Git takibinin dışında bırakılmıştır.
