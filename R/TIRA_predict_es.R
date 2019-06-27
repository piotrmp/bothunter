source("prepareGLM.R")

#registerDoMC(4)

lang='es-all'
cat("Reading data...\n")
usersT=read.table(paste0("../glm-input/users.tsv"),header=F)
testtsvT=read.table(paste0("../glm-input/test.tsv"),sep = "\t",header=T)
testcsrT=as(as.matrix.coo(read.matrix.csr(paste0("../glm-input/test.csr"))$x),"dgCMatrix")
traintsvT=readRDS(paste0("../model/train1-",lang,".rds"))
traincsrT=readRDS(paste0("../model/train2-",lang,".rds"))
cat("Computing predictibility scores...\n")
testGLM=prepareGLMtest(usersT,traintsvT,traincsrT,testtsvT,testcsrT)
testALL=testGLM
cat("Obtaining predictions...\n")
model=readRDS(paste0("../model/glm-",lang,".rds"))
pred=predict(model,testALL[,c(-1,-2)],type="response")
write.table(data.frame(testALL$user,pred),"../output-R/output.tsv",quote=F,sep="\t",row.names=F,col.names=F)
cat("Done!\n")
