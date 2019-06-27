source("prepareGLM.R")
source("prepareBERT.R")
registerDoMC(4)

lang='es'
# For GLM
usersT=read.table(paste0("../glm-input/",lang,"/users.tsv"),header=F)
traintsvT=read.table(paste0("../glm-input/",lang,"/train.tsv"),sep = "\t",header=T)
traincsrT=as(as.matrix.coo(read.matrix.csr(paste0("../glm-input/",lang,"/train.csr"))$x),"dgCMatrix")
trainGLM=prepareGLMtrain(usersT,traintsvT,traincsrT)
trainALL=trainGLM
model=glm(y~.,trainALL[,-1],family=binomial)

saveRDS(model,paste0("../model/glm-",lang,".rds"))
saveRDS(traintsvT,paste0("../model/train1-",lang,".rds"))
saveRDS(traincsrT,paste0("../model/train2-",lang,".rds"))

# For BERT
trainBERT=prepareBERT(read.table(paste0("../bert-scoring/",lang,"/train/test.tsv"),sep = "\t",header=T,quote="",comment.char=""),read.table(paste0("../bert-output/",lang,"/train/test_results.tsv"),sep = "\t",header=F))
trainALL=trainBERT
model=glm(y~.,trainALL[,-1],family=binomial)

saveRDS(model,paste0("../model/bert-",lang,".rds"))

#For both
trainALL=cbind(trainBERT,trainGLM[,c(-1,-2)])
model=glm(y~.,trainALL[,-1],family=binomial)

saveRDS(model,paste0("../model/glmbert-",lang,".rds"))
