# Release script to release Cotta
# The version of is stored in the manifest and the build number is updated 
# every time this script runs
require 'buildmaster/cotta'
require 'buildmaster/project'
require 'rake'

cotta = BuildMaster::Cotta.new
dir = cotta.file(__FILE__).parent
git = BuildMaster::Git.new(dir)
manifest_file = dir.file('core/src/META-INF/MANIFEST.MF')
manifest = BuildMaster::JavaManifest.new(manifest_file)

# git.pull(origin master
version = manifest.increase_build
git.add manifest_file.to_s.gsub(File::SEPARATOR, File::ALT_SEPARATOR || FILE::SEPARATOR)
# assuming that rake has been called load 'rake'
git.commit("releasing #{version.number}b#{version.build}")
git.tag("version-#{version.number}b#{version.build}")
dist_dir = dir.dir('build/dist')

cotta_core_jar = dist_dir.file('cotta.jar')
cotta_core_source_zip = dist_dir.file('cotta-src.zip')
cotta_testbase_jar = dist_dir.file('cotta-testbase.jar')
cotta_testbase_source_zip = dist_dir.file('cotta-testbase-src.zip')
cotta_asserts_jar = dist_dir.file('cotta-asserts.jar')
cotta_asserts_source_zip = dist_dir.file('cotta-asserts-src.zip')

cotta_core_release_jar = dist_dir.file("cotta-#{version.number}b#{version.build}.jar")
cotta_core_release_source = dist_dir.file("cotta-#{version.number}b#{version.build}-src.zip")
cotta_core_jar.copy_to(cotta_core_release_jar)
cotta_core_source_zip.copy_to(cotta_core_release_source)

cotta_testbase_release_jar = dist_dir.file("cotta-testbase-#{version.number}b#{version.build}.jar")
cotta_testbase_release_source = dist_dir.file("cotta-testbase-#{version.number}b#{version.build}-src.zip")
cotta_testbase_jar.copy_to(cotta_testbase_release_jar)
cotta_testbase_source_zip.copy_to(cotta_testbase_release_source)

cotta_asserts_release_jar = dist_dir.file("cotta-asserts-#{version.number}b#{version.build}.jar")
cotta_asserts_release_source = dist_dir.file("cotta-asserts-#{version.number}b#{version.build}-src.zip")
cotta_asserts_jar.copy_to(cotta_asserts_release_jar)
cotta_asserts_source_zip.copy_to(cotta_asserts_release_source)

pscp = BuildMaster::PscpDriver.new("wolfdancer,cotta@web.sourceforge.net")
builds_dir = '/home/groups/c/co/cotta/htdocs/builds'
report_dir = "/home/groups/c/co/cotta/htdocs/reports/#{version.number}"
javadoc_dir = "/home/groups/c/co/cotta/htdocs/javadoc/#{version.number}"

pscp.copy(cotta_core_release_jar.path, "#{builds_dir}/#{cotta_core_release_jar.name}")
pscp.copy(cotta_core_release_source.path, "#{builds_dir}/#{cotta_core_release_source.name}")
pscp.copy(cotta_testbase_release_jar, "#{builds_dir}/#{cotta_testbase_release_jar.name}")
pscp.copy(cotta_testbase_release_source, "#{builds_dir}/#{cotta_testbase_release_source.name}")
pscp.copy(cotta_asserts_release_jar, "#{builds_dir}/#{cotta_asserts_release_jar.name}")
pscp.copy(cotta_asserts_release_source, "#{builds_dir}/#{cotta_asserts_release_source.name}")

pscp.copy(dir.dir('build/report'), "#{report_dir}")
pscp.copy(dir.dir('build/dist/javadoc/asserts'), "#{javadoc_dir}")
pscp.copy(dir.dir('build/dist/javadoc/core'), "#{javadoc_dir}")

puts <<TODO
staging site
TODO
