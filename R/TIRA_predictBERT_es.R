source("prepareBERT.R")

#registerDoMC(4)

lang='es-all'
cat("Reading data...\n")
testBERT=prepareBERT(read.table(paste0("../bert-scoring/test/test.tsv"),sep = "\t",header=T,quote="",comment.char=""),read.table(paste0("../bert-output/test_results.tsv"),sep = "\t",header=F))
testALL=testBERT
cat("Obtaining predictions...\n")
model=readRDS(paste0("../model/bert-",lang,".rds"))
pred=predict(model,testALL[,c(-1,-2)],type="response")
write.table(data.frame(testALL$user,pred),"../output-R/output.tsv",quote=F,sep="\t",row.names=F,col.names=F)
cat("Done!\n")
