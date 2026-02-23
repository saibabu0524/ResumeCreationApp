BASE_LATEX_TEMPLATE = r'''\documentclass[letterpaper,11pt]{article}

\usepackage{latexsym}
\usepackage[empty]{fullpage}
\usepackage{titlesec}
\usepackage{marvosym}
\usepackage[usenames,dvipsnames]{color}
\usepackage{verbatim}
\usepackage{enumitem}
\usepackage[hidelinks]{hyperref}
\usepackage{fancyhdr}
\usepackage[english]{babel}
\usepackage{tabularx}

\pagestyle{fancy}
\fancyhf{} % clear all header and footer fields
\fancyfoot{}
\renewcommand{\headrulewidth}{0pt}
\renewcommand{\footrulewidth}{0pt}

% Adjust margins
\addtolength{\oddsidemargin}{-0.5in}
\addtolength{\evensidemargin}{-0.5in}
\addtolength{\textwidth}{1in}
\addtolength{\topmargin}{-.5in}
\addtolength{\textheight}{1.0in}

\urlstyle{same}

\raggedbottom
\raggedright
\setlength{\tabcolsep}{0in}

% Sections formatting
\titleformat{\section}{
  \vspace{-4pt}\scshape\raggedright\large
}{}{0em}{}[\color{black}\titlerule \vspace{-5pt}]

%-------------------------
% Custom commands
\newcommand{\resumeItem}[1]{
  \item\small{
    {#1 \vspace{-2pt}}
  }
}

\newcommand{\resumeSubheading}[4]{
  \vspace{-2pt}\item
    \begin{tabular*}{0.97\textwidth}[t]{l@{\extracolsep{\fill}}r}
      \textbf{#1} & #2 \\
      \textit{\small#3} & \textit{\small #4} \\
    \end{tabular*}\vspace{-7pt}
}

\newcommand{\resumeSubSubheading}[2]{
    \item
    \begin{tabular*}{0.97\textwidth}{l@{\extracolsep{\fill}}r}
      \textit{\small#1} & \textit{\small #2} \\
    \end{tabular*}\vspace{-7pt}
}

\newcommand{\resumeProjectHeading}[2]{
    \item
    \begin{tabular*}{0.97\textwidth}{l@{\extracolsep{\fill}}r}
      \small#1 & #2 \\
    \end{tabular*}\vspace{-7pt}
}

\newcommand{\resumeSubItem}[1]{\resumeItem{#1}\vspace{-4pt}}

\renewcommand\labelitemii{$\vcenter{\hbox{\tiny$\bullet$}}$}

\newcommand{\resumeSubHeadingListStart}{\begin{itemize}[leftmargin=0.15in, label={}]}
\newcommand{\resumeSubHeadingListEnd}{\end{itemize}}
\newcommand{\resumeItemListStart}{\begin{itemize}}
\newcommand{\resumeItemListEnd}{\end{itemize}\vspace{-5pt}}

%-------------------------------------------
%%%%%%  RESUME STARTS HERE  %%%%%%%%%%%%%%%%%%%%%%%%%%%%

\begin{document}

%----------HEADING----------
% \begin{tabular*}{\textwidth}{l@{\extracolsep{\fill}}r}
%   \textbf{\href{http://sourabhbajaj.com/}{\Large Sourabh Bajaj}} & Email : \href{mailto:sourabh@sourabhbajaj.com}{sourabh@sourabhbajaj.com}\\
%   \href{http://sourabhbajaj.com/}{http://www.sourabhbajaj.com} & Mobile : +1-123-456-7890 \\
% \end{tabular*}

\begin{center}
    \textbf{\Huge \scshape {{NAME}}} \\ \vspace{1pt}
    \small {{CONTACT_INFO}} \\
\end{center}

%-----------SUMMARY-----------
\section{Summary}
{{SUMMARY}}

%-----------EDUCATION-----------
\section{Education}
  \resumeSubHeadingListStart
    {{EDUCATION}}
  \resumeSubHeadingListEnd

%-----------EXPERIENCE-----------
\section{Experience}
  \resumeSubHeadingListStart
    {{EXPERIENCE}}
  \resumeSubHeadingListEnd

%-----------PROJECTS-----------
\section{Projects}
    \resumeSubHeadingListStart
      {{PROJECTS}}
    \resumeSubHeadingListEnd

%-----------TECHNICAL SKILLS-----------
\section{Technical Skills}
 \begin{itemize}[leftmargin=0.15in, label={}]
    \small{\item{
     {{SKILLS}}
    }}
 \end{itemize}

%-------------------------------------------
\end{document}
'''

STAGE_A_PROMPT = """
You are an expert resume formatter. Your task is to take the raw text extracted from a user's resume and format it exactly into the provided LaTeX template.
Preserve EVERY piece of information without paraphrasing.
Use standard resume sections: Summary, Experience, Skills, Education, Projects.
If a section is missing from the raw text, leave its placeholder empty or remove the section from the LaTeX.
Ensure you properly escape LaTeX special characters like &, %, $, #, _, {{, }}, ~, ^, \.
Output ONLY valid LaTeX code wrapped exactly in <latex>...</latex> tags. Do not output anything else.

Here is the base LaTeX template containing placeholders like {{NAME}}, {{CONTACT_INFO}}, {{SUMMARY}}, {{EDUCATION}}, {{EXPERIENCE}}, {{PROJECTS}}, {{SKILLS}}:
```latex
{latex_template}
```

Raw Resume Text:
```
{resume_text}
```
"""

STAGE_B_PROMPT = """
You are an expert technical recruiter and resume writer. 
Your task is to tailor a candidate's LaTeX resume to perfectly match a provided Job Description.

Instructions:
1. Reorder sections and skills to match the Job Description priorities.
2. Mirror keywords and terminology from the Job Description in the bullet points.
3. Rewrite the Summary paragraph to position the candidate perfectly for this specific role.
4. DO NOT fabricate any experience, certifications, or skills that the candidate doesn't have.
5. DO NOT remove any experience or projects, only reword and re-prioritize.
6. Make sure all LaTeX syntax remains valid and error-free. Be very careful with special characters like &, %, $, #, _, {{, }}, ~, ^, \. Ensure they are escaped.
7. Output ONLY valid LaTeX code wrapped exactly in <latex>...</latex> tags. Do not output anything else.

Job Description:
```
{job_description}
```

Original LaTeX Resume:
```latex
{latex_resume}
```
"""
