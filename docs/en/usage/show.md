# Display Summary Information {#ShowMode}

Use the following command to display summary information from the GTB:

```bash
show <input> [options]
```

This command is used to quickly access the information of a GTB file and output it to the terminal. `show` mode contains three output functions:

- Output the summary information about the GTB file (such as shape of genotypes, GTBNodes, etc.).
- Output the sample names of the GTB file.
- Output the summary information (e.g., coordinates, allele labels, allele frequency, etc.) for the variants that satisfy the filtering criteria (if specified).

For detailed genotypes and diverse output formats, please use the `extract` mode.

## Program Options {#Options}

```bash
Usage: show <input> [options]
Options:
  --contig  Specify the corresponding contig file.
            default: /contig/human/hg38.p13
            format: --contig <file> (Exists,File,Inner)
Summary View Options:
  --add-md5      Print the message-digest fingerprint (checksum) for file 
                 (which may take a long time to calculating for huge files).
  --add-subject  Print subjects names of the GTB file.
  --add-tree     Print information of the GTBTrees (chromosome only by 
                 default). 
  --add-node     Print information of the GTBNodes.
  --full,-f      Print all abstract information of the GTB file (i.e., 
                 --list-baseInfo, --list-subject, --list-node).
GTB View Options:
  --list-subject-only   Print subjects names of the GTB file only.
  --list-position-only  Print coordinates (i.e., CHROM,POSITION) of the GTB 
                        file only.
  --list-site-only      Print coordinates, alleles and INFOs (i.e., 
                        CHROM,POSITION,REF,ALT,INFO) of the GTB file.
Subset Selection Options:
  --subject,-s   Print the information of the specified subjects. Subject name 
                 can be stored in a file with ',' delimited form, and pass in 
                 via '-s @file'.
                 format: --subject <string>,<string>,...
  --range,-r     Print the information by position range.
                 format: --range <chrom>:<minPos>-<maxPos> 
                 <chrom>:<minPos>-<maxPos> ...
  --random       Print the information by position. (An inputFile is needed 
                 here, with each line contains 'chrom,position' or 'chrom 
                 position'. 
                 format: --random <file>
  --retain-node  Print variants in the specified coordinate range of the 
                 specified chromosome.
                 format: --retain-node <string>:<int>-<int> 
                 <string>:<int>-<int> ...
  --seq-ac       Exclude variants with the alternate allele count (AC) per 
                 variant out of the range [minAc, maxAc].
                 format: --seq-ac <int>-<int> (>= 0)
  --seq-af       Exclude variants with the alternate allele frequency (AF) per 
                 variant out of the range [minAf, maxAf].
                 format: --seq-af <double>-<double> (0.0 ~ 1.0)
  --seq-an       Exclude variants with the non-missing allele number (AN) per 
                 variant out of the range [minAn, maxAn].
                 format: --seq-an <int>-<int> (>= 0)
  --max-allele   Exclude variants with alleles over --max-allele.
                 default: 15
                 format: --max-allele <int> (2 ~ 15)
```

## Example {#Examples}

Use the GBC to list the summary information for the GTB file `./example/assoc.hg19.gtb` and set the following properties.

- Print all GTB nodes' abstract information.
- Print the MD5 checksum of the GTB.

The commands to complete the task are as follows:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
show ./example/assoc.hg19.gtb \
--full --add-md5

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc extract ./example/assoc.hg19.gtb show ./example/assoc.hg19.gtb --full --add-md5
```

Here, the terminal prints the following message:

```bash
Summary of GTB File:
  GTB File Name: /Users/suranyi/Documents/project/GBC/GBC-1.1/example/assoc.hg19.gtb
  GTB File Size: 938.092 KB
  Genome Reference: ftp://ftp.1000genomes.ebi.ac.uk//vol1/ftp/technical/reference/phase2_reference_assembly_sequence/hs37d5.fa.gz
  MD5 Code: dcb53e6a9844d413e05ea2a95cae7289
  Suggest To BGZF: false
  Phased: true
  Ordered GTB: true
  BlockSize: 16384 (-bs 7)
  Compression Level: 16 (ZSTD)
  Dimension of Genotypes: 1 chromosome, 18339 variants and 983 subjects
  Subject Sequence: HG01583 HG01586 HG01589 HG01593 HG02490 HG02491 HG02493 HG02494 HG02597 HG02600
                   HG02601 HG02603 HG02604 HG02648 HG02649 HG02651 HG02652 HG02654 HG02655 HG02657
                   HG02658 HG02660 HG02661 HG02681 HG02682 HG02684 HG02685 HG02687 HG02688 HG02690
                   HG02691 HG02694 HG02696 HG02697 HG02699 HG02700 HG02724 HG02725 HG02727 HG02728
                   HG02731 HG02733 HG02734 HG02736 HG02737 HG02774 HG02775 HG02778 HG02780 HG02783
                   HG02784 HG02786 HG02787 HG02789 HG02790 HG02792 HG02793 HG03006 HG03007 HG03009
                   HG03012 HG03015 HG03016 HG03018 HG03019 HG03021 HG03022 HG03228 HG03229 HG03234
                   HG03235 HG03237 HG03238 HG03488 HG03490 HG03491 HG03585 HG03589 HG03593 HG03594
                   HG03595 HG03598 HG03600 HG03603 HG03604 HG03607 HG03611 HG03615 HG03616 HG03619
                   HG03624 HG03625 HG03629 HG03631 HG03634 HG03636 HG03640 HG03642 HG03643 HG03644
                   HG03645 HG03646 HG03649 HG03652 HG03653 HG03660 HG03663 HG03667 HG03668 HG03672
                   HG03673 HG03679 HG03680 HG03681 HG03684 HG03685 HG03686 HG03687 HG03689 HG03690
                   HG03691 HG03692 HG03693 HG03694 HG03695 HG03696 HG03697 HG03698 HG03702 HG03703
                   HG03705 HG03706 HG03708 HG03709 HG03711 HG03713 HG03714 HG03716 HG03717 HG03718
                   HG03720 HG03722 HG03727 HG03729 HG03730 HG03731 HG03733 HG03736 HG03738 HG03740
                   HG03741 HG03742 HG03743 HG03744 HG03745 HG03746 HG03750 HG03752 HG03753 HG03754
                   HG03755 HG03756 HG03757 HG03760 HG03762 HG03765 HG03767 HG03770 HG03771 HG03772
                   HG03773 HG03774 HG03775 HG03777 HG03778 HG03779 HG03780 HG03781 HG03782 HG03784
                   HG03785 HG03786 HG03787 HG03788 HG03789 HG03790 HG03792 HG03793 HG03796 HG03800
                   HG03802 HG03803 HG03805 HG03808 HG03809 HG03812 HG03814 HG03815 HG03817 HG03821
                   HG03823 HG03824 HG03826 HG03829 HG03830 HG03832 HG03833 HG03836 HG03837 HG03838
                   HG03844 HG03846 HG03848 HG03849 HG03850 HG03851 HG03854 HG03856 HG03857 HG03858
                   HG03861 HG03862 HG03863 HG03864 HG03866 HG03867 HG03868 HG03869 HG03870 HG03871
                   HG03872 HG03873 HG03874 HG03875 HG03882 HG03884 HG03885 HG03886 HG03887 HG03888
                   HG03890 HG03894 HG03895 HG03896 HG03897 HG03898 HG03899 HG03900 HG03902 HG03905
                   HG03907 HG03908 HG03910 HG03911 HG03913 HG03914 HG03916 HG03917 HG03919 HG03920
                   HG03922 HG03925 HG03926 HG03928 HG03931 HG03934 HG03937 HG03940 HG03941 HG03943
                   HG03945 HG03947 HG03949 HG03950 HG03951 HG03953 HG03955 HG03960 HG03963 HG03965
                   HG03967 HG03968 HG03969 HG03971 HG03973 HG03974 HG03976 HG03977 HG03978 HG03985
                   HG03986 HG03989 HG03990 HG03991 HG03995 HG03998 HG03999 HG04001 HG04002 HG04003
                   HG04006 HG04014 HG04015 HG04017 HG04018 HG04019 HG04020 HG04022 HG04023 HG04025
                   HG04026 HG04029 HG04033 HG04035 HG04038 HG04039 HG04042 HG04047 HG04054 HG04056
                   HG04059 HG04060 HG04061 HG04062 HG04063 HG04070 HG04075 HG04076 HG04080 HG04090
                   HG04093 HG04094 HG04096 HG04098 HG04099 HG04100 HG04106 HG04107 HG04118 HG04131
                   HG04134 HG04140 HG04141 HG04144 HG04146 HG04152 HG04153 HG04155 HG04156 HG04158
                   HG04159 HG04161 HG04162 HG04164 HG04171 HG04173 HG04176 HG04177 HG04180 HG04182
                   HG04183 HG04185 HG04186 HG04188 HG04189 HG04194 HG04195 HG04198 HG04200 HG04202
                   HG04206 HG04209 HG04210 HG04211 HG04212 HG04214 HG04216 HG04219 HG04222 HG04225
                   HG04227 HG04229 HG04235 HG04238 HG04239 NA20845 NA20846 NA20847 NA20849 NA20850
                   NA20851 NA20852 NA20853 NA20854 NA20856 NA20858 NA20859 NA20861 NA20862 NA20863
                   NA20864 NA20866 NA20867 NA20868 NA20869 NA20870 NA20872 NA20875 NA20876 NA20877
                   NA20878 NA20881 NA20882 NA20884 NA20885 NA20886 NA20887 NA20888 NA20889 NA20890
                   NA20891 NA20892 NA20894 NA20895 NA20896 NA20897 NA20899 NA20900 NA20901 NA20902
                   NA20903 NA20904 NA20905 NA20906 NA20908 NA20911 NA21086 NA21087 NA21088 NA21089
                   NA21090 NA21091 NA21092 NA21093 NA21094 NA21095 NA21097 NA21098 NA21099 NA21100
                   NA21101 NA21102 NA21103 NA21104 NA21105 NA21106 NA21107 NA21108 NA21109 NA21110
                   NA21111 NA21112 NA21113 NA21114 NA21115 NA21116 NA21117 NA21118 NA21119 NA21120
                   NA21122 NA21123 NA21124 NA21125 NA21126 NA21127 NA21128 NA21129 NA21130 NA21133
                   NA21135 NA21137 NA21141 NA21142 NA21143 NA21144 HG00403 HG00404 HG00406 HG00407
                   HG00409 HG00410 HG00419 HG00421 HG00422 HG00428 HG00436 HG00437 HG00442 HG00443
                   HG00445 HG00446 HG00448 HG00449 HG00451 HG00452 HG00457 HG00458 HG00463 HG00464
                   HG00472 HG00473 HG00475 HG00476 HG00478 HG00479 HG00500 HG00513 HG00525 HG00530
                   HG00531 HG00533 HG00534 HG00536 HG00537 HG00542 HG00543 HG00556 HG00557 HG00559
                   HG00560 HG00565 HG00566 HG00580 HG00583 HG00589 HG00590 HG00592 HG00593 HG00595
                   HG00596 HG00598 HG00599 HG00607 HG00608 HG00610 HG00611 HG00613 HG00614 HG00619
                   HG00620 HG00622 HG00623 HG00625 HG00626 HG00628 HG00629 HG00631 HG00632 HG00634
                   HG00650 HG00651 HG00653 HG00654 HG00656 HG00657 HG00662 HG00663 HG00671 HG00672
                   HG00674 HG00675 HG00683 HG00684 HG00689 HG00690 HG00692 HG00693 HG00698 HG00699
                   HG00701 HG00704 HG00705 HG00707 HG00708 HG00717 HG00728 HG00729 HG00759 HG00766
                   HG00844 HG00851 HG00864 HG00879 HG00881 HG00956 HG00982 HG01028 HG01029 HG01031
                   HG01046 HG01595 HG01596 HG01597 HG01598 HG01599 HG01600 HG01794 HG01795 HG01796
                   HG01797 HG01798 HG01799 HG01800 HG01801 HG01802 HG01804 HG01805 HG01806 HG01807
                   HG01808 HG01809 HG01810 HG01811 HG01812 HG01813 HG01815 HG01816 HG01817 HG01840
                   HG01841 HG01842 HG01843 HG01844 HG01845 HG01846 HG01847 HG01848 HG01849 HG01850
                   HG01851 HG01852 HG01853 HG01855 HG01857 HG01858 HG01859 HG01860 HG01861 HG01862
                   HG01863 HG01864 HG01865 HG01866 HG01867 HG01868 HG01869 HG01870 HG01871 HG01872
                   HG01873 HG01874 HG01878 HG02016 HG02017 HG02019 HG02020 HG02023 HG02025 HG02026
                   HG02028 HG02029 HG02031 HG02032 HG02035 HG02040 HG02047 HG02048 HG02049 HG02050
                   HG02057 HG02058 HG02060 HG02061 HG02064 HG02069 HG02070 HG02072 HG02073 HG02075
                   HG02076 HG02078 HG02079 HG02081 HG02082 HG02084 HG02085 HG02086 HG02087 HG02088
                   HG02113 HG02116 HG02121 HG02122 HG02127 HG02128 HG02130 HG02131 HG02133 HG02134
                   HG02136 HG02137 HG02138 HG02139 HG02140 HG02141 HG02142 HG02151 HG02152 HG02153
                   HG02154 HG02155 HG02156 HG02164 HG02165 HG02166 HG02178 HG02179 HG02180 HG02181
                   HG02182 HG02184 HG02185 HG02186 HG02187 HG02188 HG02190 HG02250 HG02351 HG02353
                   HG02355 HG02356 HG02360 HG02364 HG02367 HG02371 HG02374 HG02375 HG02379 HG02380
                   HG02382 HG02383 HG02384 HG02385 HG02386 HG02389 HG02390 HG02391 HG02392 HG02394
                   HG02395 HG02396 HG02397 HG02398 HG02399 HG02401 HG02402 HG02406 HG02407 HG02408
                   HG02409 HG02410 HG02512 HG02513 HG02521 HG02522 NA18525 NA18526 NA18528 NA18530
                   NA18531 NA18532 NA18533 NA18534 NA18535 NA18536 NA18537 NA18538 NA18539 NA18541
                   NA18542 NA18543 NA18544 NA18545 NA18546 NA18547 NA18548 NA18549 NA18550 NA18552
                   NA18553 NA18555 NA18557 NA18558 NA18559 NA18560 NA18561 NA18562 NA18563 NA18564
                   NA18565 NA18566 NA18567 NA18570 NA18571 NA18572 NA18573 NA18574 NA18577 NA18579
                   NA18582 NA18591 NA18592 NA18593 NA18595 NA18596 NA18597 NA18599 NA18602 NA18603
                   NA18605 NA18606 NA18608 NA18609 NA18610 NA18611 NA18612 NA18613 NA18614 NA18615
                   NA18616 NA18617 NA18618 NA18619 NA18620 NA18621 NA18622 NA18623 NA18624 NA18625
                   NA18626 NA18627 NA18628 NA18629 NA18630 NA18631 NA18632 NA18633 NA18634 NA18635
                   NA18636 NA18637 NA18638 NA18639 NA18640 NA18641 NA18642 NA18643 NA18644 NA18645
                   NA18646 NA18647 NA18648 NA18740 NA18745 NA18747 NA18748 NA18749 NA18757 NA18939
                   NA18940 NA18941 NA18942 NA18943 NA18944 NA18945 NA18946 NA18947 NA18948 NA18949
                   NA18950 NA18951 NA18952 NA18953 NA18954 NA18956 NA18957 NA18959 NA18960 NA18961
                   NA18962 NA18963 NA18964 NA18965 NA18966 NA18967 NA18968 NA18969 NA18970 NA18971
                   NA18972 NA18973 NA18974 NA18975 NA18976 NA18977 NA18978 NA18979 NA18980 NA18981
                   NA18982 NA18983 NA18984 NA18985 NA18986 NA18987 NA18988 NA18989 NA18990 NA18991
                   NA18992 NA18993 NA18994 NA18995 NA18997 NA18998 NA18999 NA19000 NA19001 NA19002
                   NA19003 NA19004 NA19005 NA19006 NA19007 NA19009 NA19010 NA19011 NA19012 NA19054
                   NA19055 NA19056 NA19057 NA19058 NA19059 NA19060 NA19062 NA19063 NA19064 NA19065
                   NA19066 NA19067 NA19068 NA19070 NA19072 NA19074 NA19075 NA19076 NA19077 NA19078
                   NA19079 NA19080 NA19081 NA19082 NA19083 NA19084 NA19085 NA19086 NA19087 NA19088
                   NA19089 NA19090 NA19091 
  
Summary of GTB Nodes:
└─ Chromosome 1: posRange=[10177, 4999852], numOfNodes=4, numOfVariants=18339
    ├─ Node 1: posRange=[10177, 1787378], seek=1157, blockSize=241089, variantNum=4430 (4430 + 0)
    ├─ Node 2: posRange=[1788685, 2914618], seek=242246, blockSize=228815, variantNum=3998 (3998 + 0)
    ├─ Node 3: posRange=[2915041, 4063039], seek=471061, blockSize=249262, variantNum=4818 (4818 + 0)
    └─ Node 4: posRange=[4063447, 4999852], seek=720323, blockSize=240183, variantNum=5093 (5093 + 0)
```

Use the GBC to list the variants in the GTB file `. /example/assoc.hg19.gtb` that meet the following conditions:

- Print the variants with $$\text{POS}\in [0,100000]$$.
- Print the variants with $$\text{AF}\in[0.45, 0.55]$$.
- Limit the samples to NA18963,NA18977,HG02401,HG02353,HG02064.

The commands to complete the task are as follows:

```bash
# Linux or MacOS
docker run -v `pwd`:`pwd` -w `pwd` --rm -it -m 500m gbc \
show ./example/assoc.hg19.gtb \
-r 1:0-100000 --seq-af 0.45-0.55 -s NA18963,NA18977,HG02401,HG02353,HG02064 --list-site-only

# Windows
docker run -v %cd%:%cd% -w %cd% --rm -it -m 500m gbc show ./example/assoc.hg19.gtb -r 1:0-100000 --seq-af 0.45-0.55 -s NA18963,NA18977,HG02401,HG02353,HG02064 --list-site-only
```

Here, the terminal prints the following message:

```bash
1    15211    T    G        AC=5;AF=0.50000000;AN=10
1    54712    T    TTTTC    AC=5;AF=0.50000000;AN=10
```

