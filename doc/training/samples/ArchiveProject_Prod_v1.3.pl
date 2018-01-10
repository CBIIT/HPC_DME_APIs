#!/usr/bin/perl -w
use strict;
use Cwd;
use File::Spec;


#ArchiveProject v1.3
#  Notes:
#       +because this script is compartmentalized, metadata will be first written to csv tables before being converted to temporary json files for registration/upload
#       +for individually demultiplexed samples with pool names in the LIMS/Metadata, an association file should be used (with options -t and -p) containing 2 tab-delimited columns: SampleName and PoolName
#  v1.1:
#    --modified to allow script to be submitted as a job to the clusters
#    --added NAS attribute
#    --modified to allow 10X archiving (i.e. more than 2 FASTQ files per sample)
#  v1.2:
#    --added BAM files archive option -d
#    --added minor error detection: commas in PI name are not allowed, since they may signify multiple PIs for the project
#    --added scan for the /out folder within project directories (i.e. for 10x single cell projects)
#    --added scan for the /tophat_alignment folder within project directories (i.e. for tophat projects)
#    --added options -t and -p to deal with pooled data
#  v1.3:
#    --add demultiplex html file to Flowcell level collections when it exists
#    --object names and object symbolic paths now have file extensions (e.g. DataObject_bamfile1.bam)

# defaults, initializations, and constants
my $version = '1.3';
my $help = "\nArchiveProject_Prod v$version\nPerforms three steps to archive a project into the CCR-SF Cleversafe vault using the Metadata report.\n".
            "\nNotes:\n".
            " +because this script is compartmentalized, metadata will be first written to csv tables before being converted to temporary json files for registration/upload\n".
            " +for individually demultiplexed samples with pool names in the LIMS/Metadata, an association file should be used (with options -t and -p) containing 2 tab-delimited columns: SampleName and PoolName\n".
            "\nExample 1 (archive FASTQ files):\nArchiveProject_Prod.pl -i ProjectName_Flowcellname_Metadata.txt -f 161207_D00761_0099_BC9VPWANXX -u Unaligned/ProjectName/ -x Authentication.txt\n".
            "\nExample 2 (archive FASTQ & BAM files):\nqsub_atrf.pl \"ArchiveProject_Prod.pl -i ProjectName_Flowcellname_Metadata.txt -f 161207_D00761_0099_BC9VPWANXX -u Unaligned/ProjectName/ -d Analysis/ProjectName/ -x Authentication.txt 2>archive_dme.err\" 8\n".
          "\n ### Main Options-Switches: commonly used or required ###\n".
          "\t-i  Option: specify archive table input file. Default=STDIN.\n\t    Note: if m=3 or m=4, input file is output of m=2.\n".
          "\t-u  Option: specify project demultiplex directory. Required.\n\t    E.g. -u Unaligned/ProjectName/\n".
          "\t-f  Option: specify run name. Required.\n".
          "\t-x  Option: specify token/authentication file to use or create. Required.\n".
          "\t-a  Option: authentication mode. Default=0.
                  0: Use existing token (i.e. use existing config file).
                  1: Generate new token and exit (i.e. creates/edits config file).\n".
          "\t-d  Option: specify project analysis directory. Optional.\n\t     E.g. -d Analysis/ProjectName/\n".
          "\n ### Advanced Options-Switches: rarely needed and optional ###\n".
          "\t-o  Option: specify output log file. Default='[InputFileName]_Archive.log'.\n".
          "\t-m  Option: run mode. Default=1.
                  1: perform all actions.
                  2: create archive files only (gzips, md5s, and collection/object tables).
                  3: register metadata only.
                  4: upload archive files only.\n".
          "\t-z  Option: overwrite mode. Default=0.
                  0: no overwriting: skip already registered metadata
                     only unregistered data files will be uploaded.
                  1: Full Overwriting: register/update all project metadata
                     attempt to upload all data files.
                  2: Metadata Overwriting: register/update all metadata
                     no data files will be uploaded.
                  3: Collections Overwriting: register/update Collections metadata only
                     ignore Data Objects metadata and data files.\n".
          "\t-t  Option: Data Object type. Default=0.
                  0: individual FASTQ files (i.e. sample names in LIMS).
                  1: unpooled FASTQ files (i.e. pool names in LIMS).\n".
          "\t-p  Option: Specify sample/pool association file. Required if t=1.\n".
          "\n************\nWritten by:\tCris Vera\nGroup:\t\tCCR-SF Bioinformatics Group\nContact:\tSFILLUMINABIOINF\@mail.nih.gov\n";
my $usage = "\nUsage:\nArchiveProject_Prod.pl -i [Input Archive Table File Name] -o [Output Log File Name] -u [Demultiplex Directory] -m [Run Mode] -f [Run Name] -a [Authentication Mode] -x [Config File Name] -t [Data Object Type] -p [Sample/Pool Association File]\n";
my $infh = 'STDIN';
my $infile;
my $rundir = getcwd;
my $mode = 1;
my $date = GetDate();
my $sleep = 10;  #delay between collection registrations (i.e. allows server time to update)
my $i = 0; #metadata samples
my $x = 0; #samples that are matching (i.e. exist in both the project Demultiplex directory and in the Metadata Table)
my $y = 0; #unmatched samples
my $xx = 0; #samples that are matching, unregistered in DME, and written to the collection table (i.e. final cut)
my $s = 0; #samples in the project Demultiplex directory
my $f = 0; #total files in project Demultiplex directory
my $b = 0; #total files in project Analysis directory
my $ff = 0; #total data objects that are matching, unregistered in DME, and written to the data object table (i.e. final cut)
my $spath = '/FNL_SF_Archive';
my ($indir,$reportdir,$bamdir,$runname,$flowcellname,$projectdir,$rundate,$excludedsamples,$userid,$filename,$logfile,$poolfile);
my $overwrite = 0;
my $tarobjects = 0;
my $cred = 0;
my $bams = 0;
#my $HPCserver = 'https://hpcdmeapi.nci.nih.gov';
my $HPCserver = 'https://hpcdmeapi.nci.nih.gov:8080';  #HPC DME server address
my $client = '/is2/projects/CCR-SF/scratch/illumina/Processing/ANALYSIS/DATA/verajc/test/HPC/HPCclient4/java -jar hpc-cli-1.0.jar';  ##not currently in use
my $configfile = '/is2/projects/CCR-SF/scratch/illumina/Processing/ANALYSIS/DATA/verajc/HPC_config.txt'; #default authentication config file
my $configtemplate = '/is2/projects/CCR-SF/active/Software/scripts/bin/HPC_config'; #template authentication config file
my $authenticate = '/is2/projects/CCR-SF/active/Software/scripts/bin/authenticate_prod.sh'; #authentication token generating shell script
my (@samplenames,@sampledirs,@samplefiles,@bamdirs,@bamfiles);
my %samplefiles;

#header hash: collection to metadata table.  Currently not in use
#my %headers = (
#  "sample_id" => "SampleID", #sample
#  "pi_name" => "PrincipalInvestigator", #PI
#  "contact_name" => "LabContact", #project
#  "project_description" => "ProjectName", #project
#  "bioinformatics_contact" => "BioinformaticsContact", #project
#  "project_start_date" => "ProjectOpenDate", #project
#  "sample_name" => "SampleName", #sample
#  "sequencing_application_type" => "Application", #flowcell
#  "reference_genome" => "ReferenceGenome", #data object
#  "project_id_CSAS" => "CSAS", #project
#  "sequencing_platform" => "MachineType", #flowcell
#  "read_length" => "ReadLength", #flowcell
#  "library_name" => "LibraryKit", #sample
#  "initial_sample_volume" => "SubmittedSampleVolumeUL", #sample
#  "initial_sample_concentration" => "SubmittedSampleConc", #sample
#  "sfqc_sample_concentration" => "LabQCSampleConc", #sample
#  "sfqc_library_concentration" => "LibraryConc", #sample
#  "sfqc_library_size" => "ConsensusLibrarySize", #sample
#  "flowcell_id" => "Flowcell", #flowcell
#  "run_name" => "RunName", #flowcell
#  "RIN" => "RIN", #sample
#  "pooling" => "Pooling", #flowcell
#  "Project_ID_NAS" => "NAS", #project
#  
#  #missing metadata table values (reversed)
#  "LibraryConcUnit" => "na", #sample
#  "CurrentSampleVolumeUL" => "na", #sample
#  "SubmittedSampleConcUnit" => "na", #sample
#  "SampleType" => "na", #sample
#  "DateReceived" => "na", #sample
#  "LabQCSampleConcUnit" => "na", #sample
#);

#process command line custom script tags
my %cmds = ReturnCmds(@ARGV);
die "\n$help\n$usage\n" if ($cmds{'h'});
if ($cmds{'i'}) {
  $infh = 'IN';
  $infile = $cmds{'i'};
  open ($infh, "<$infile") or die "Cannot open $infile: $!\n";
  $logfile = $filename = $infile;
  $logfile =~ s/_Metadata\.txt/_Archive.log/;
  $filename =~ s/_Metadata\.txt//;
}
if ($cmds{'o'}) {
  $logfile = $cmds{'o'};
}
$indir = $reportdir = $cmds{'u'} if ($cmds{'u'});
$bamdir = $cmds{'d'} if ($cmds{'d'});
$mode = $cmds{'m'} if ($cmds{'m'});
$runname = $cmds{'f'} if ($cmds{'f'});
$overwrite = $cmds{'z'} if ($cmds{'z'});
$cred = $cmds{'a'} if ($cmds{'a'});
$configfile = $cmds{'x'} if ($cmds{'x'});
$tarobjects = $cmds{'t'} if ($cmds{'t'});
$poolfile = $cmds{'p'} if ($cmds{'p'});
$bams = 1 if ($bamdir);

##get absolute paths for directories
$reportdir =~ s/\/$//; #remove trailing /
$indir = $reportdir = File::Spec->rel2abs($indir);
$configfile = File::Spec->rel2abs($configfile) if ($configfile);
$poolfile = File::Spec->rel2abs($poolfile) if ($poolfile);
$bamdir = File::Spec->rel2abs($bamdir) if ($bamdir);
$logfile = File::Spec->rel2abs($logfile);

#open log file
if (-e $logfile){
  open ('LOG', ">>$logfile") or die "Cannot append $logfile: $!\n";
  print LOG "*****************\n***RE-RUNNING***\n*****************\nArchiveProject_Prod.pl v$version\nDate: $date\nInput Metadata File: $cmds{'i'}\nProject Directory: $indir\nProject Run Mode: $mode\nOverwrite Mode: $overwrite\n\n";
}
else{
  open ('LOG', ">$logfile") or die "Cannot create $logfile: $!\n";
  print LOG "ArchiveProject.pl v$version\nDate: $date\nInput Metadata File: $cmds{'i'}\nProject Directory: $indir\nProject Run Mode: $mode\nOverwrite Mode: $overwrite\n\n";
}

##copy/edit authentication token config file, add new token, and then exit
if ($cred == 1){
  print STDERR "\nEnter HPC/NIH User Name: ";
  $userid = <STDIN>;
  chomp $userid;
  `cp $configtemplate $configfile` if (!-e $configfile); #copy template config file to working dir
  `$authenticate $userid $configfile`;  #run authentication script
  print STDERR "\nCreate new HPC authentication token: done\nAuthentification Config File: $configfile\n\n";
  print LOG "\nCreate new HPC authentication token: done\nAuthentification Config File: $configfile\n\n";
  print LOG "\nExiting Script...\n\n";
  die "\nExiting Script...\n\n";
}
else{
  print LOG "\nError: cannot find Authentification Config File: $configfile\n\n" if (!-e $configfile);
  die "\nError: cannot find Authentification Config File: $configfile\n\n" if (!-e $configfile);
  print STDERR "\nCreate new HPC authentication token: skipped\nAuthentification Config File: $configfile\n\n";
  print LOG "\nCreate new HPC authentication token: skipped\nAuthentification Config File: $configfile\n\n";
}

##drop when required options are undefined
die "\nError: required option not specified.\n" if (!$indir or !$runname);

##determine flowcell name from run name
if ($runname =~ m/([0-9]{6,6})_[A-Z0-9]+_[0-9]+_[AB]([A-Z0-9]+)$/){
  $flowcellname = $2;
  $rundate = DateFormat($1);
}
elsif ($runname =~ m/([0-9]{6,6})_[A-Z0-9]+_[0-9]+_([0-9]+\-[A-Z0-9]+)$/){
  $flowcellname = $2;
  $rundate = DateFormat($1);
}
else{
  die "\nError: unknown run name format: $runname\n\n";
}

##get project folder name and html path##
if ($reportdir =~ s/\/([^\/]+)$//){
  $projectdir = $1;
  $reportdir .= "/Unaligned/Reports/html/$flowcellname/$projectdir/all/all/laneBarcode.html";
}
else{
  print LOG "\nError: project name not found in -u: $reportdir\n\n";
  die "\nError: project name not found in -u: $reportdir\n\n";
}

####modify metadata text file using pool2sample file###
if ($tarobjects and $poolfile and ($mode == 1 or $mode == 2)){
  my %metadata;
  my ($samplecol); #sample name column in metadata
  print LOG "\n###Modifying Metadata Table according to pool2sample file:\n";
  print LOG "\nError: cannot use STDIN for option -i with option -t 1.\n\n" and die "\nError: cannot use STDIN for option -i with option -t 1.\n\n" if ($infh eq 'STDIN');
  open ('MET', ">$infile.Samples") or die "Cannot create $infile.Samples: $!\n";
  while (my $row = <$infh>){
    $i += 1;
    chomp $row;
    my @row = split /\t/,$row;
    if ($i == 1){
      $samplecol = Col('SampleName',@row);
      print MET "$row\n";
    }
    else{
      $metadata{$row[$samplecol]} = $row;
    }
  }
  close $infh;
  my $ii = 0;
  open (TAR, "<$poolfile") or (print LOG "\nError: Cannot open pool2sample file: $poolfile: $!\n" and die "Cannot open $poolfile: $!\n");
  while (my $line = <TAR>){
    chomp $line;
    next if ($line =~ m/^\#/);
    $ii += 1;
    my @line = split /\t/,$line;
    if ($metadata{$line[1]}){
      my @meta = split /\t/,$metadata{$line[1]};
      splice @meta,$samplecol,1,$line[0];
      $line = join "\t",@meta;
      print MET "$line\n";
    }
  }
  close(TAR);
  close(MET);
  print STDERR "\nNew Metadata Table created: $infile.Samples\n\twith # samples: $ii\n\tFrom # pools: $i\n\n";
  print LOG "\nNew Metadata Table created: $infile.Samples\n\twith # samples: $ii\n\tFrom # pools: $i\n\n";
  open ($infh, "<$infile.Samples") or die "Cannot open $infile.Samples: $!\n";
  $i = 0;
}
###


##scan demultiplex dir
print LOG "\n###Scan Demultiplex directory:\n";
opendir(INDIR,$indir) or (print LOG "Can't open directory: $indir: $!\n" and die "Can't open directory: $indir: $!\n");
@sampledirs = grep {m/^Sample_/ && -d "$indir/$_"} readdir(INDIR);
close(INDIR);
print LOG "Error: no sample folders detected in Unaligned directory: $indir\n\n" if (scalar @sampledirs == 0);
die "Error: no sample folders detected in Demultiplex directory: $indir\n\n" if (scalar @sampledirs == 0);
##scan Analysis dir
if ($bams == 1){
  print LOG "\n###Scan Analysis directory:\n";
  opendir(BAMDIR,$bamdir) or (print LOG "Can't open directory: $bamdir: $!\n" and die "Can't open directory: $bamdir: $!\n");
  @bamdirs = grep {m/^Sample_/ && -d "$bamdir/$_"} readdir(BAMDIR);
  close(BAMDIR);
  my $comp = CompareLists(\@sampledirs,\@bamdirs);
  print LOG "\nError: Sample folders missing from project analysis directory: $comp\n\n" if ($comp);
  die "\nError: Sample folders missing from project analysis directory: $comp\n\n" if ($comp);
}

##loop through sample dirs and do file preps
foreach my $sampledir (@sampledirs){
  @bamfiles = ();
  my $name = $sampledir;
  my $samplebamdir = $sampledir;
  $name =~ s/Sample_//;
  push @samplenames,$name;
  opendir(INDIR,"$indir/$sampledir") or die "Can't open directory: $indir/$sampledir: $!\n";
  @samplefiles = grep {m/(\.gz$)|(\.fastq$)|(\.md5$)/ && -f "$indir/$sampledir/$_"} readdir(INDIR);
  close(INDIR);
  if ($bams){
    $samplebamdir .= '/outs' if (-e "$bamdir/$samplebamdir/outs"); #for single cell projects
    $samplebamdir .= '/tophat_alignment' if (-e "$bamdir/$samplebamdir/tophat_alignment"); #for tophat projects
    opendir(BAMDIR,"$bamdir/$samplebamdir") or die "Can't open directory: $bamdir/$samplebamdir: $!\n";
    ##get bam index files (they're unique to the deliverable BAMs)
    @bamfiles = grep {m/(\.bai$)|(\.bai\.md5$)/ && -f "$bamdir/$samplebamdir/$_"} readdir(BAMDIR);
    print LOG "\nError: multiple BAM file deliverables detected for sample: $name\n\n" if (scalar @bamfiles > 2);
    die "\nError: multiple BAM file deliverables detected for sample: $name\n\n" if (scalar @bamfiles > 2);
    ##add bam files
    my @temp;
    foreach my $index (@bamfiles){
      my $bam = $index;
      next if ($bam =~ m/\.md5/i);
      $bam =~ s/\.bai$//i;
      push @temp,$bam if (-e "$bamdir/$samplebamdir/$bam");
      print LOG "\nError: corresponding BAM file not found for index: $index\n\n" if (!(-e "$bamdir/$samplebamdir/$bam"));
      die "\nError: corresponding BAM file not found for index: $index\n\n" if (!(-e "$bamdir/$samplebamdir/$bam"));
      push @temp,"$bam.md5" if (-e "$bamdir/$samplebamdir/$bam.md5");
    }
    push @bamfiles,@temp;
    @temp = ();
    close(BAMDIR);
  }
  foreach my $file (@samplefiles){
    $file = "$indir/$sampledir/$file";
    my $test = "$file.md5";
    $samplefiles{$name} .= ",$file" if (exists $samplefiles{$name});
    $samplefiles{$name} = $file if (!exists $samplefiles{$name});
    $f += 1;
    ##gzip files and run md5sum
    if ($mode == 1 or $mode == 2){
      if ($file =~ m/\.fastq$/){
        print STDERR "\nGzipping sample $name FASTQ file: $file\n";
        print LOG "\nGzipping $name FASTQ file: $file\n";
        `gzip $file`;
        $file .= '.gz';
      }
      if ($file !~ m/\.md5/i and !(-e $test)){
        print STDERR "\nCreating MD5 file for sample $name FASTQ file: $file\n";
        print LOG "\nCreating MD5 file for sample $name FASTQ file: $file\n";
        `md5sum $file >$file.md5`;
        $samplefiles{$name} .= ",$file.md5" if (exists $samplefiles{$name});
        $samplefiles{$name} = "$file.md5" if (!exists $samplefiles{$name});
        $f += 1;
      }
    }
  }
  if ($bams){
    foreach my $file (@bamfiles){
      $file = "$bamdir/$samplebamdir/$file";
      my $test = "$file.md5";
      $samplefiles{$name} .= ",$file" if (exists $samplefiles{$name});
      $samplefiles{$name} = $file if (!exists $samplefiles{$name});
      $b += 1;
      ##run md5sum
      if ($mode == 1 or $mode == 2){
        if ($file !~ m/\.md5/i and !(-e $test)){
          print STDERR "\nCreating MD5 file for sample $name BAM file: $file\n";
          print LOG "\nCreating MD5 file for sample $name BAM file: $file\n";
          `md5sum $file >$file.md5`;
          $samplefiles{$name} .= ",$file.md5" if (exists $samplefiles{$name});
          $samplefiles{$name} = "$file.md5" if (!exists $samplefiles{$name});
          $b += 1;
        }
      }
    }
  }
  $s += 1;
}
print STDERR "\nTotal samples found in project Demultiplex directory: $s\n";
print STDERR "Total FASTQ files found in project Demultiplex directory (including MD5s): $f\n";
print STDERR "Total BAM and INDEX files in project Analysis directory (including MD5s): $b\n" if ($bams);
print STDERR "Demultiplex HTML file found: yes\n\n" if (-e $reportdir);
print STDERR "Demultiplex HTML file found: no\n\n" if (!-e $reportdir);
print LOG "\nTotal samples in project Demultiplex directory: $s\n";
print LOG "Total FASTQ files in project Demultiplex directory (including MD5s): $f\n";
print LOG "Total BAM and INDEX files in project Analysis directory (including MD5s): $b\n\n" if ($bams);
print LOG "Demultiplex HTML file found: yes\n\n" if (-e $reportdir);
print LOG "Demultiplex HTML file found: no\n\n" if (!-e $reportdir);

##perform collection and data object table creation
if ($mode == 1 or $mode == 2){
  print LOG "\n###Perform collection and data object table creation:\n";
  ##headers
  my (@inheaders);
  my @outheaders1_old = ("collection_type", "collection_path", "name", "pi_name", "contact_name", "project_id_CSAS", "project_description", "bioinformatics_contact",
                  "project_start_date", "grant_funding_agent", "flowcell_id", "run_name", "run_date", "sequencing_platform", "sequencing_application_type",
                  "sequencing_run_protocol", "read_length", "sample_id", "sample_name", "initial_sample_concentration", "initial_sample_volume", "sfqc_sample_concentration",
                  "sfqc_sample_volume", "sfqc_sample_size", "library_id", "library_name", "sfqc_library_concentration", "sfqc_library_volume", "sfqc_library_size",
                  "source_id", "source_name", "source_organism", "source_provider");
  my @outheaders1 = ("collection_type", "collection_path", "pi_name", "project_id_CSAS_NAS", "project_name", "contact_name", "bioinformatics_contact",
                  "project_start_date", "grant_funding_agent", "flowcell_id", "run_name", "run_date", "sequencing_platform", "sequencing_application_type",
                  "read_length", "pooling", "sample_id", "sample_name", "initial_sample_concentration_ngul", "initial_sample_volume_ul", "sfqc_sample_concentration_ngul",
                  "sfqc_sample_size", "RIN", "28s18s", "library_id", "library_name", "library_lot", "sfqc_library_concentration_nM", "sfqc_library_size",
                  "source_id", "source_name", "source_organism", "source_provider");
  my @outheaders2_old = ("object_path", "name", "description", "file_type", "reference_genome", "reference_annotation", "software_tool", "md5_checksum", "phi_content",
                     "pii_content", "data_encryption_status", "data_compression_status", "fileId");
  my @outheaders2 = ("object_path", "object_name", "file_type", "reference_genome", "reference_annotation", "software_tool", "md5_checksum", "phi_content",
                     "pii_content", "data_encryption_status", "data_compression_status", "fileId");
  my $outheaders1 = join ",",@outheaders1;
  my $outheaders2 = join ",",@outheaders2;
  
  ##cycle through metadata table
  while (my $row = <$infh>){
    chomp $row;
    $i += 1;
    my @row = split /\t/,$row;
    ##remove commas, end spaces
    foreach my $field (@row){
      $field =~ s/,/;/g;
      $field =~ s/ *$//;
    }
    if ($i == 1){
      @inheaders = @row;
      ##Metadata table headers
      #SampleID	PrincipalInvestigator	LabContact	ProjectName	BioinformaticsContact	ProjectOpenDate	SampleName	SampleType	Application	ReferenceGenome	CSAS_NAS
      #MachineType	ReadLength	Pooling	LibraryKit	SubmittedSampleVolumeUL	SubmittedSampleConc	SubmittedSampleConcUnit	CurrentSampleVolumeUL	DateReceived
      #LabQCSampleConc	LabQCSampleConcUnit	RIN	28s18s	RnaArea	SamplePeakSize	SamplePeakSizeUnit	SampleRegionSize	SampleRegionSizeUnit	LibraryConc
      #LibraryConcUnit	ConsensusLibrarySize	ConsensusLibrarySizeUnit	LibraryPeakSize	LibraryPeakSizeUnit	LibraryRegionSize	LibraryRegionSizeUnit Software
      
      ##old DME collection headers
      #collection_type	collection_path	name	pi_name	contact_name	project_id_CSAS	project_description	bioinformatics_contact	project_start_date
      #grant_funding_agent	flowcell_id	run_name	run_date	sequencing_platform	sequencing_application_type	sequencing_run_protocol	read_length
      #sample_id	sample_name	initial_sample_concentration	initial_sample_volume	sfqc_sample_concentration	sfqc_sample_volume_	sfqc_sample_size
      #library_id	library_name	sfqc_library_concentration	sfqc_library_volume	sfqc_library_size	source_id	source_name	source_organism	source_provider
      
      ##new collection headers
      #collection_type	collection_path	pi_name project_id_CSAS_NAS	project_name	lab_contact  bioinformatics_contact	project_start_date
      #grant_funding_agent	flowcell_id	run_name	run_date	sequencing_platform	sequencing_ sequencing_application_type	read_length pooling
      #sample_id	sample_name	initial_sample_concentration_ngul	initial_sample_volume_ul	sfqc_sample_concentration_ngul	sfqc_sample_size
      #RIN  28s18s  library_id	library_lot library_name	sfqc_library_concentration_nM	sfqc_library_size	source_id	source_name	source_organism	source_provider
      
      ##old DME dataobject headers
      #object_path	name  description	file_type	reference_genome	reference_annotation	software_tool	md5_checksum	phi_content	pii_content	data_encryption_status
      #data_compression_status	fileId  *fileContainerId*

      ##new dataobject headers
      #object_path	object_name	file_type	reference_genome	reference_annotation	software_tool	md5_checksum	phi_content	pii_content	data_encryption_status
      #data_compression_status	fileId  *fileContainerId*
      
      unlink "Collections_$filename.csv" if (-e "Collections_$filename.csv");
      unlink "DataObjects_$filename.csv" if (-e "DataObjects_$filename.csv");
    }
    else{
      my $samplename = $row[Col('SampleName',@inheaders)];
      my $projectname = $row[Col('ProjectName',@inheaders)];
      my $PIname = $row[Col('PrincipalInvestigator',@inheaders)];
      $PIname =~ s/ /_/g;
      $PIname =~ s/\.//g;
      print LOG "\nError: PI Name has comma: $PIname\n\tAction: please edit Metadata txt file to remove commas from 'PrincipalInvestigator' field.\n\n" if ($PIname =~ m/[,;]/);
      die "\nError: PI Name has comma: $PIname\n\tAction: please edit Metadata txt file to remove commas from 'PrincipalInvestigator' field.\n\n" if ($PIname =~ m/[,;]/);
      my $PIsymbol = "$spath/PI_Lab_$PIname";
      my $projectsymbol = "$PIsymbol/Project_$projectname";
      my $flowcellsymbol = "$projectsymbol/Flowcell_$flowcellname";
      my $samplesymbol = "$flowcellsymbol/Sample_$samplename";
      my $reference = my $org = $row[Col('ReferenceGenome',@inheaders)];
      my $annotation = 'Unknown';
      $annotation = 'ENSEMBL_v70' if ($reference =~ m/(human)|(hg19)/i);
      $annotation = 'ENSEMBL_GRCh38_v79' if ($reference =~ m/hg38/i);
      $annotation = 'ENSEMBL_NCBI37_mm9' if ($reference =~ m/(mouse)|(mm9)/i);
      $annotation = 'ENSEMBL_GRCm38_mm10' if ($reference =~ m/mm10/i);
      
      #check to make sure metadata table entry exists, is in Demultiplex directory, and has existing data object(s) ready for archival
      if ($samplefiles{$samplename} and CheckList($samplename,@samplenames)){
        my @dataobjects = split /,/,$samplefiles{$samplename};
        print LOG "\nError: file content mismatch:\n\tInput file name: $filename\n\tLIMS file name: $projectname\_$flowcellname\n\n" if ($filename ne "$projectname\_$flowcellname");
        die "\nError: file content mismatch:\n\tInput file name: $filename\n\tLIMS file name: $projectname\_$flowcellname\n\n" if ($filename ne "$projectname\_$flowcellname");
        $filename = "$projectname\_$flowcellname";
        open ('OUT1', ">>Collections_$filename.csv") or die "Cannot create Collections_$filename.csv: $!\n";
        open ('OUT2', ">>DataObjects_$filename.csv") or die "Cannot create DataObjects_$filename.csv: $!\n";
        if ($x == 0){
          print OUT1 "$outheaders1\n";
          print OUT2 "$outheaders2\n";
          
          #check to see if PI is registered. Add PI_Lab collection to temp file if not registered
          if ($overwrite >= 1 or !SearchLevel('PI_Lab',$PIsymbol,$configfile,$HPCserver,$filename)){
            print OUT1 "PI_Lab,$PIsymbol,$PIname\n";
            print LOG "PI_Lab Level Collection: $PIsymbol\n\tNot Found in HPC server: adding to Collections Table.\n" if (!$overwrite);
            print LOG "PI_Lab Level Collection: $PIsymbol\n\tOverwrite/Update mode active: adding to Collections Table.\n" if ($overwrite);
          }
          else{
            print LOG "PI_Lab Level Collection: $PIsymbol\n\tAlready Exists in HPC server: NOT adding to Collections Table.\n" if (!$overwrite);
          }
          #check to see if Project is registered
          if ($overwrite >= 1 or !SearchLevel('Project',$projectsymbol,$configfile,$HPCserver,$filename)){
            print OUT1 "Project,$projectsymbol,,";
            print OUT1 $row[Col('CSAS_NAS',@inheaders)].",$projectname,".$row[Col('LabContact',@inheaders)].",".$row[Col('BioinformaticsContact',@inheaders)].",".$row[Col('ProjectOpenDate',@inheaders)].",NIH\n";
            print LOG "Project Level Collection: $projectsymbol\n\tNot Found in HPC server: adding to Collections Table.\n" if (!$overwrite);
            print LOG "Project Level Collection: $projectsymbol\n\tOverwrite/Update mode active: adding to Collections Table.\n" if ($overwrite);
          }
          else{
            print LOG "Project Level Collection: $projectsymbol\n\tAlready Exists in HPC server: NOT adding to Collections Table.\n" if (!$overwrite);
          }
          #check to see if flowcell is registered
          if ($overwrite >= 1 or !SearchLevel('Flowcell',$flowcellsymbol,$configfile,$HPCserver,$filename)){
            print OUT1 "Flowcell,$flowcellsymbol,,,,,,,,";
            print OUT1 "$flowcellname,$runname,$rundate,".$row[Col('MachineType',@inheaders)].",".$row[Col('Application',@inheaders)].",".$row[Col('ReadLength',@inheaders)].",".$row[Col('Pooling',@inheaders)]."\n";
            print LOG "Flowcell Level Collection: $flowcellsymbol\n\tNot Found in HPC server: adding to Collections Table.\n" if (!$overwrite);
            print LOG "Flowcell Level Collection: $flowcellsymbol\n\tOverwrite/Update mode active: adding to Collections Table.\n" if ($overwrite);
          }
          else{
            print LOG "Flowcell Level Collection: $flowcellsymbol\n\tAlready Exists in HPC server: NOT adding to Collections Table.\n" if (!$overwrite);
          }
          #check to see if first sample is registered
          if ($overwrite >= 1 or !SearchLevel('Sample',$samplesymbol,$configfile,$HPCserver,$filename)){
            print OUT1 "Sample,$samplesymbol,,,,,,,,,,,,,,,";
            print OUT1 $row[Col('SampleID',@inheaders)].",$samplename,".$row[Col('SubmittedSampleConc',@inheaders)].",".$row[Col('SubmittedSampleVolumeUL',@inheaders)].",".$row[Col('LabQCSampleConc',@inheaders)].
            ",".$row[Col('SampleRegionSize',@inheaders)].",".$row[Col('RIN',@inheaders)].",".$row[Col('28s18s',@inheaders)].",na,".$row[Col('LibraryKit',@inheaders)].",na,".$row[Col('LibraryConc',@inheaders)].",".$row[Col('ConsensusLibrarySize',@inheaders)].
            ",sourceid,sourcename,$org,sourceprov\n";
            $xx += 1; #samples written to collections table
            print LOG "Sample Level Collection: $samplesymbol\n\tNot Found in HPC server: adding to Collections Table.\n" if (!$overwrite);
            print LOG "Sample Level Collection: $samplesymbol\n\tOverwrite/Update mode active: adding to Collections Table.\n" if ($overwrite);
          }
          else{
            print LOG "Sample Level Collection: $samplesymbol\n\tAlready Exists in HPC server: NOT adding to Collections Table.\n" if (!$overwrite);
          }
          
          ####check if Demultiplex HTML data objsect is registered at the Flowcell level####
          if (-e $reportdir){
            my $objectname = $reportdir;
            $objectname =~ s/.+\/([^\/]+)$/$1/;
            $objectname =~ s/\./_/g;        ##remove all periods
            $objectname =~ s/_([^_]+)/.$1/; ##keep file extension format for data objects
            my $objectsymbol = "$flowcellsymbol/DataObject_$objectname";
            if ($overwrite == 1 or $overwrite == 2 or ($overwrite != 3 and !SearchLevel('DataObject',$objectsymbol,$configfile,$HPCserver,$filename))){
              my $desc = "HTML";
              print OUT2 "$objectsymbol,$objectname,$desc,$reference,$annotation,".$row[Col('Software',@inheaders)].",no,,,,Not Compressed,$reportdir\n";
              print LOG "Data Object: $objectsymbol\n\tNot Found in HPC server: adding to DataObject Table.\n" if (!$overwrite);
              print LOG "Data Object: $objectsymbol\n\tOverwrite/Update mode active: adding to DataObject Table.\n" if ($overwrite == 1 or $overwrite == 2);
              $ff += 1;
            }
            else{
              print LOG "Data Object: $objectsymbol\n\tAlready Exists in HPC server: NOT adding to Data Object Table.\n" if (!$overwrite);
              print LOG "Data Object: $objectsymbol\n\tOverwrite mode 3 active: NOT adding to Data Object Table.\n" if ($overwrite == 3);
            }
          }
          
          #check if sample level data obects are registered
          foreach my $object (@dataobjects){
            my $objectname = $object;
            $objectname =~ s/.+\/([^\/]+)$/$1/;
            $objectname =~ s/\./_/g;        ##remove all periods
            $objectname =~ s/_([^_]+)/.$1/; ##keep file extension format for data objects
            my $objectsymbol = "$samplesymbol/DataObject_$objectname";
            if ($overwrite == 1 or $overwrite == 2 or ($overwrite != 3 and !SearchLevel('DataObject',$objectsymbol,$configfile,$HPCserver,$filename))){
              my $desc;
              $desc = "FASTQ" if ($objectname =~ m/_fastq.gz$/);
              $desc = "MD5SUM" if ($objectname =~ m/.md5$/);
              $desc = "BAM" if ($objectname =~ m/.bam$/);
              $desc = "INDEX" if ($objectname =~ m/.bai$/);
              print OUT2 "$objectsymbol,$objectname,$desc,$reference,$annotation,".$row[Col('Software',@inheaders)];
              print OUT2 ",yes" if ($desc eq 'MD5SUM');
              print OUT2 ",no" if ($desc ne 'MD5SUM');
              print OUT2 ",,,,Compressed" if ($object =~ m/\.gz$/);
              print OUT2 ",,,,Not Compressed" if ($object !~ m/\.gz$/);
              print OUT2 ",$object\n";
              $ff += 1;
              print LOG "Data Object: $objectsymbol\n\tNot Found in HPC server: adding to DataObject Table.\n" if (!$overwrite);
              print LOG "Data Object: $objectsymbol\n\tOverwrite/Update mode active: adding to DataObject Table.\n" if ($overwrite == 1 or $overwrite == 2);
            }
            else{
              print LOG "Data Object: $objectsymbol\n\tAlready Exists in HPC server: NOT adding to Data Object Table.\n" if (!$overwrite);
              print LOG "Data Object: $objectsymbol\n\tOverwrite mode 3 active: NOT adding to Data Object Table.\n" if ($overwrite == 3);
            }
          }
        }
        else{
          #print out remaining collection samples
          if ($overwrite >= 1 or !SearchLevel('Sample',$samplesymbol,$configfile,$HPCserver,$filename)){
            print OUT1 "Sample,$samplesymbol,,,,,,,,,,,,,,,";
            print OUT1 $row[Col('SampleID',@inheaders)].",$samplename,".$row[Col('SubmittedSampleConc',@inheaders)].",".$row[Col('SubmittedSampleVolumeUL',@inheaders)].",".$row[Col('LabQCSampleConc',@inheaders)].
            ",".$row[Col('SampleRegionSize',@inheaders)].",".$row[Col('RIN',@inheaders)].",".$row[Col('28s18s',@inheaders)].",na,".$row[Col('LibraryKit',@inheaders)].",na,".$row[Col('LibraryConc',@inheaders)].",".$row[Col('ConsensusLibrarySize',@inheaders)].
            ",sourceid,sourcename,$org,sourceprov\n";
            $xx += 1; #samples written to collections table
            print LOG "Sample Level Collection: $samplesymbol\n\tNot Found in HPC server: adding to Collections Table.\n" if (!$overwrite);
            print LOG "Sample Level Collection: $samplesymbol\n\tOverwrite/Update mode active: adding to Collections Table.\n" if ($overwrite);
          }
          else{
            print LOG "Sample Level Collection: $samplesymbol\n\tAlready Exists in HPC server: NOT adding to Collections Table.\n" if (!$overwrite);
          }
          #check if remaining sample data obects are registered
          foreach my $object (@dataobjects){
            my $objectname = $object;
            $objectname =~ s/.+\/([^\/]+)$/$1/;
            $objectname =~ s/\./_/g;        ##remove all periods
            $objectname =~ s/_([^_]+)/.$1/; ##keep file extension format for data objects
            my $objectsymbol = "$samplesymbol/DataObject_$objectname";
            if ($overwrite == 1 or $overwrite == 2 or ($overwrite != 3 and !SearchLevel('DataObject',$objectsymbol,$configfile,$HPCserver,$filename))){
              my $desc;
              $desc = "FASTQ" if ($objectname =~ m/.fastq_gz$/);
              $desc = "MD5SUM" if ($objectname =~ m/.md5$/);
              $desc = "BAM" if ($objectname =~ m/.bam$/);
              $desc = "INDEX" if ($objectname =~ m/.bai$/);
              print OUT2 "$objectsymbol,$objectname,$desc,$reference,$annotation,".$row[Col('Software',@inheaders)];
              print OUT2 ",yes" if ($desc eq 'MD5SUM');
              print OUT2 ",no" if ($desc ne 'MD5SUM');
              print OUT2 ",,,,Compressed" if ($object =~ m/\.gz$/);
              print OUT2 ",,,,Not Compressed" if ($object !~ m/\.gz$/);
              print OUT2 ",$object\n";
              $ff += 1;
              print LOG "Data Object: $objectsymbol\n\tNot Found in HPC server: adding to DataObject Table.\n" if (!$overwrite);
              print LOG "Data Object: $objectsymbol\n\tOverwrite/Update mode active: adding to DataObject Table.\n" if ($overwrite == 1 or $overwrite == 2);
            }
            else{
              print LOG "Data Object: $objectsymbol\n\tAlready Exists in HPC server: NOT adding to Data Object Table.\n" if (!$overwrite);
              print LOG "Data Object: $objectsymbol\n\tOverwrite mode 3 active: NOT adding to Data Object Table.\n" if ($overwrite == 3);
            }
          }
        }
        $x += 1; #matching samples (with objects) found
        close(OUT1);
        close(OUT2);
      }
      else{
        $y += 1; #extra project samples in LIMS
        $excludedsamples .= ", $samplename" if ($excludedsamples);
        $excludedsamples = $samplename if (!$excludedsamples);
      }
    }
  }
  $i -= 1; #remove header line
  close($infh);
  print STDERR "\nTotal Metadata Table entries: $i\n";
  print STDERR "Table entries with existing Demultiplex directory samples: $x\n";
  print STDERR "Samples written to Collections Table: $xx\n";
  print STDERR "$y Metadata Table entries ignored: $excludedsamples\n" if ($y);
  print STDERR "Objects (i.e. files) written to Data Objects Table: $ff\n\n";
  print LOG "\nTotal Metadata Table entries: $i\n";
  print LOG "Table entries with existing Demultiplex directory samples: $x\n";
  print LOG "Samples written to Collections Table: $xx\n";
  print LOG "$y Metadata Table entries ignored: $excludedsamples\n" if ($y);
  print LOG "Objects (i.e. files) written to Data Objects Table: $ff\n\n";
}

##run collection registration
if ($mode == 1 or $mode == 3){
  print LOG "\n###Collection Registration:\n";
  if ($mode == 3){
    $filename =~ s/^Collections_//;
    $filename =~ s/^DataObjects_//;
    $filename =~ s/\.[^.]+$//;
  }
  open (IN, "<Collections_$filename.csv") or die "Cannot open Collections_$filename.csv: $!\n";
  chomp (my @collections = <IN>);
  close (IN);
  my $headers = shift(@collections);
  $i = 0;
  foreach my $collection (@collections){
    next if (!$collection);
    $i += 1;
    my $type = (split(/,/,$collection))[0];
    my $json = "$type\_$filename\_Temp.json";
    my $path = CreateJson($type,$collection,$headers,$json);
    print LOG "\nError: json file creation failed: $json\n\n" if (!$path);
    die "\nError: json file creation failed: $json\n\n" if (!$path);
    print LOG "\nCollection Registration Curl command $i: curl -s -k -H \"Content-Type: application/json\" -d \@$json -X PUT $HPCserver/collection$path -H \"Accept: application/json\" -D output_header_registerCollection_$filename.log -o output_message_registerCollection_$filename.json --config $configfile\n";
    `curl -s -k -H "Content-Type: application/json" -d \@$json -X PUT $HPCserver/collection$path -H "Accept: application/json" -D output_header_registerCollection_$filename.log -o output_message_registerCollection_$filename.json --config $configfile`;
    my @error = CheckCurlHeader("output_header_registerCollection_$filename.log");
    print LOG "\nError: curl command failed to register collection $i:\n\ttype: $type\n\tHPCpath: $path\n\terror: $error[1]\n\terror file: output_message_registerCollection_$filename.json\n\n" if (!$error[0]);
    die "\nError: curl command failed to register collection $i:\n\ttype: $type\n\tHPCpath: $path\n\terror: $error[1]\n\terror file: output_message_registerCollection_$filename.json\n\n" if (!$error[0]);
    print STDERR "\nCollection $i successfully registered:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 5);
    print LOG "\nCollection $i successfully registered:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 5);
    print STDERR "\nCollection $i successfully updated:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 2);
    print LOG "\nCollection $i successfully updated:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 2);
    unlink $json;
  }
  unlink "output_header_registerCollection_$filename.log";
  unlink "output_message_registerCollection_$filename.json";
  print STDERR "\nTotal collections successfully registered: $i\n\n";
  print LOG "\nTotal collections successfully registered: $i\n\n";
}

##run dataset registration and data upload to archive
if ($mode == 1 or $mode == 4){
  print LOG "\n###data object registration and/or data file upload to Cleversafe:\n";
  if ($mode == 4){
    $filename =~ s/^Collections_//;
    $filename =~ s/^DataObjects_//;
    $filename =~ s/\.[^.]+$//;
  }
  open (IN, "<DataObjects_$filename.csv") or die "Cannot open DataObjects_$filename.csv: $!\n";
  chomp (my @dataobjects = <IN>);
  close (IN);
  my $headers = shift(@dataobjects);
  $i = 0;
  foreach my $dataobject (@dataobjects){
    $i += 1;
    my $type = 'DataObject';
    my $json = "$type\_$filename\_Temp.json";
    my ($path,$file) = split /,/,(CreateJson($type,$dataobject,$headers,$json));
    print LOG "\nError: json file creation failed: $json\n\n" if (!$path);
    die "\nError: json file creation failed: $json\n\n" if (!$path);
    if (!$overwrite or $overwrite == 1){
      print LOG "\nData Object Registration and File Upload Curl command $i: curl -s -k -F \"dataObjectRegistration=\@$json;type=application/json\" -F \"dataObject=\@$file\" -X PUT $HPCserver/dataObject$path -D output_header_registerDataObject_$filename.log -o output_message_registerDataObject_$filename.json --config $configfile\n";
      `curl -s -k -F "dataObjectRegistration=\@$json;type=application/json" -F "dataObject=\@$file;type=application/octet-stream" -X PUT $HPCserver/dataObject$path -D output_header_registerDataObject_$filename.log -o output_message_registerDataObject_$filename.json --config $configfile`;
    }
    elsif ($overwrite == 2){
      print LOG "\nData Object Registration Only Curl command $i: curl -s -k -F \"dataObjectRegistration=\@$json;type=application/json\" -X PUT $HPCserver/dataObject$path -D output_header_registerDataObject_$filename.log -o output_message_registerDataObject_$filename.json --config $configfile\n";
      `curl -s -k -F "dataObjectRegistration=\@$json;type=application/json" -X PUT $HPCserver/dataObject$path -D output_header_registerDataObject_$filename.log -o output_message_registerDataObject_$filename.json --config $configfile`;
    }
    my @error = CheckCurlHeader("output_header_registerDataObject_$filename.log");
    print LOG "\nError: curl command failed to register collection $i:\n\ttype: $type\n\tHPCpath: $path\n\terror: $error[1]\n\terror file: output_message_registerCollection_$filename.json\n\n" if (!$error[0]);
    die "\nError: curl command failed to register collection $i:\n\ttype: $type\n\tHPCpath: $path\n\terror: $error[1]\n\terror file: output_message_registerCollection_$filename.json\n\n" if (!$error[0]);
    print STDERR "\nData Object $i successfully registered and archived!:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 3);
    print STDERR "\nData Object $i successfully registered:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 4);
    print LOG "\nData Object $i successfully registered and archived!:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 3);
    print LOG "\nData Object $i successfully registered:\n\ttype: $type\n\tHPCpath: $path\n" if ($error[0] == 4);
    unlink $json;
  }
  unlink "output_header_registerDataObject_$filename.log";
  unlink "output_message_registerDataObject_$filename.json";
  print STDERR "\nTotal data objects successfully registered: $i\n\n";
  print LOG "\nTotal data objects successfully registered: $i\n\n";
}

sub CreateJson{
  my ($type,$object,$headers,$jsonfile) = @_;
  my @object = split /,/,$object;
  my @headers = split /,/,$headers;
  my ($path,$file);
  my $globus = 0;
  my %json;
  my $n = 0;
  foreach my $header (@headers){
    my $nn = $n;
    $nn = '0'.$nn if ($nn < 10);
    $json{"$nn$header"} = $object[$n] if ($object[$n]);
    $globus = 1 if ($header eq 'fileContainerId' and $object[$n]);
    $n += 1;
  }
  $n = 0;
  open ('JSON', ">$jsonfile") or die "Cannot create $jsonfile: $!\n";
  print JSON '{';
  if ($type eq 'DataObject' and $globus){
    print JSON '"source":{';
    print JSON '"fileContainerId":"'.$json{'fileContainerId'}.'",},';
    print JSON '"fileId":"'.$json{'fileId'}.'"},';
  }
  print JSON '"metadataEntries":[';
  foreach my $tag (sort keys %json){
    my $attribute = $tag;
    $attribute =~ s/^[0-9]{2,2}//;
    if ($attribute eq 'collection_path'){
      $path = $json{$tag};
      next;
    }
    elsif ($attribute eq 'object_path'){
      $path = $json{$tag};
      next;
    }
    elsif ($attribute eq 'fileId'){
      $file = $json{$tag};
      next;
    }
    elsif ($attribute eq 'fileContainerId'){
      next;
    }
    print JSON ',' if ($n);
    print JSON '{"attribute":"'.$attribute.'",';
    print JSON '"value":"'.$json{$tag}.'"}';
    $n += 1;
  }
  print JSON ']}';
  close(JSON);
  return "$path,$file" if ($n > 1 and $type eq 'DataObject');
  return $path if ($n > 1);
  return 0;
}

#determine if a symbolic path is present in DME or not using curl commands.
sub SearchLevel{
  my ($levelname,$symbolname,$config,$server,$name) = @_;
  my ($object);
  my $rundir = getcwd;
  if ($levelname ne 'DataObject'){
    print LOG "\nHPC Check Collection:curl -k -s -G -X GET https://fr-s-hpcdm-uat-p.ncifcrf.gov:7738/hpc-server/collection$symbolname --config $config -H \"Accept: application/json\" -D $rundir/TempHeaderLog_$name.tmp\n";
    $object = `curl -k -s -G -X GET $server/collection$symbolname --config $config -H "Accept: application/json" -D $rundir/TempHeaderLog_$name.tmp`;
    my @check = CheckCurlHeader("$rundir/TempHeaderLog_$name.tmp");
    die "\nError: Failed to successfully access HPC server: $check[1]\n\n" if (!$check[0]);
    return 0 if ($check[0] == 1);
    return 1 if ($check[0] == 2 and $object =~ m/^\{\"collections\"\:\[\{\"collection\"\:\{\"collectionId\"\:/);
  }
  elsif ($levelname eq 'DataObject'){
    $object = `curl -k -s -G -X GET $server/dataObject$symbolname --config $config -H "Accept: application/json" -D $rundir/TempHeaderLog_$name.tmp`;
    my @check = CheckCurlHeader("$rundir/TempHeaderLog_$name.tmp");
    die "\nError: Failed to successfully access HPC server: $check[1]\n\n" if (!$check[0]);
    return 0 if ($check[0] == 1);
    return 1 if ($check[0] == 2 and $object =~ m/^\{\"dataObjects\"\:\[\{\"dataObject\"\:\{\"id\"\:/);
  }
  die "\nError: Unexpected object at Sub SearchLevel: $object\n\n";
}

sub CheckCurlHeader{
  my ($file) = @_;
  open (HED, "<$file") or die "Sub CheckCurlHeader: Cannot open $file: $!\n";
  chomp(my @lines = <HED>);
  close(HED);
  #unlink $file;
  if ($lines[0] =~ m/204 no content/i){
    return 1;
  }
  elsif ($lines[0] =~ m/200 ok/i){
    return 2;
  }
  elsif ($lines[0] =~ m/100 continue/i and $lines[2] =~ m/201 created/i){
    return 3;
  }
  elsif ($lines[0] =~ m/100 continue/i and $lines[2] =~ m/200 ok/i){
    return 4;
  }
  elsif ($lines[0] =~ m/201 created/i){
    return 5;
  }
  my @error = (0,$lines[0]);
  return @error;
}

sub CheckList{
  my ($searchitem,@list) = @_;
  foreach my $listitem (@list){
    return 1 if ($listitem eq $searchitem);
  }
  return 0;
}
sub CompareLists{
  my ($isthislist,$inthislist) = @_;
  my $results;
  foreach my $item1 (@{$isthislist}){
    $results .= ", $item1" if ($results and !CheckList($item1,@{$inthislist}));
    $results .= $item1 if (!$results and !CheckList($item1,@{$inthislist}));
  }
  return $results if ($results);
  return 0;
}
sub Col{
  my ($name,@headers) = @_;
  my $i = 0;
  foreach my $header (@headers){
    return $i if ($name eq $header);
    $i += 1;
  }
  die "\nError: at sub Col: no such header: $name\n\n";
}

sub ReturnCmds{
  my (@cmds) = @_;
  my ($opt);
  my %cmds;
  foreach my $cmd (@cmds){
    if (!$opt and $cmd =~ m/^-([a-zA-Z])/) {
      $opt = $1;
    }
    elsif ($opt and $cmd =~ m/^-([a-zA-Z])/){
      $cmds{$opt} = 1;
      $opt = $1;
    }
    elsif ($opt){
      $cmds{$opt} = $cmd;
      $opt = '';
    }
  }
  $cmds{$opt} = 1 if ($opt);
  return %cmds;
}

#convert run date to xx/xx/xxxx format
sub DateFormat{
  my ($indate) = @_;
  if ($indate =~ m/^([0-9]{2,2})([0-9]{2,2})([0-9]{2,2})$/){
    return "$2/$3/20$1";
  }
  return 0;
}

sub CheckJobs{
    my (@jobinfo) = @_;
    foreach my $job (@jobinfo){
        if ($job =~ m/\/(NGS[0-9]+)\.job/) {
            my $id = $1;
            return 0 if (!-e "$id.b.err");
        }
        else{
            die "\nError: job ID not found.\n";
        }
    }
    return 1;
}

sub GetDate{
  my ($sec,$min,$hour,$mday,$mon,$year,$wday,$yday,$isdst) = localtime(time);
  my @months = qw(January February March April May June July August September October November December);
  $year += 1900;
  my $date = "$months[$mon] $mday, $year";
  return $date;
}
